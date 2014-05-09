package com.qubole.qds.sdk.java.details;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.qubole.qds.sdk.java.api.ClusterApi;
import com.qubole.qds.sdk.java.api.CommandApi;
import com.qubole.qds.sdk.java.api.DbTapApi;
import com.qubole.qds.sdk.java.api.HiveMetadataApi;
import com.qubole.qds.sdk.java.api.ReportsApi;
import com.qubole.qds.sdk.java.api.SchedulerApi;
import com.qubole.qds.sdk.java.client.QdsClient;
import com.qubole.qds.sdk.java.client.QdsConfiguration;
import org.codehaus.jackson.map.ObjectMapper;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class QdsClientImpl implements QdsClient
{
    private final QdsConfiguration configuration;
    private final Client client;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final WebTarget target;
    private final CommandApiImpl commandApi;
    private final ClusterApiImpl clusterApi;
    private final HiveMetadataApiImpl hiveMetadataApi;
    private final DbTapApiImpl dbTapsApi;
    private final ReportsApiImpl reportsApi;
    private final SchedulerApiImpl schedulerApi;

    private static final ObjectMapper mapper = new ObjectMapper();

    static ObjectMapper getMapper()
    {
        return mapper;
    }

    public QdsClientImpl(QdsConfiguration configuration)
    {
        this.configuration = Preconditions.checkNotNull(configuration, "configuration cannot be null");
        client = configuration.newClient();
        target = client.target(configuration.getApiEndpoint()).path(configuration.getApiVersion());
        commandApi = new CommandApiImpl(this);
        clusterApi = new ClusterApiImpl(this);
        hiveMetadataApi = new HiveMetadataApiImpl(this);
        dbTapsApi = new DbTapApiImpl(this);
        reportsApi = new ReportsApiImpl(this);
        schedulerApi = new SchedulerApiImpl(this);
    }

    @Override
    public ClusterApi cluster()
    {
        return clusterApi;
    }

    @Override
    public CommandApi command()
    {
        return commandApi;
    }

    @Override
    public HiveMetadataApi hiveMetadata()
    {
        return hiveMetadataApi;
    }

    @Override
    public DbTapApi dbTaps()
    {
        return dbTapsApi;
    }

    @Override
    public ReportsApi reports()
    {
        return reportsApi;
    }

    @Override
    public SchedulerApi scheduler()
    {
        return schedulerApi;
    }

    @Override
    public <T> Future<T> invokeRequest(ForPage forPage, ClientEntity entity, Class<T> responseType, String... additionalPaths)
    {
        AsyncInvoker invoker = prepareRequest(forPage, entity, additionalPaths);
        return invokePreparedRequest(entity, responseType, invoker);
    }

    @Override
    public <T> Future<T> invokeRequest(ForPage forPage, ClientEntity entity, GenericType<T> responseType, String... additionalPaths)
    {
        AsyncInvoker invoker = prepareRequest(forPage, entity, additionalPaths);
        return invokePreparedRequest(entity, responseType, invoker);
    }

    @VisibleForTesting
    protected <T> Future<T> invokePreparedRequest(ClientEntity entity, Class<T> responseType, AsyncInvoker invoker)
    {
        if ( entity != null )
        {
            if ( entity.getEntity() != null )
            {
                return invoker.method(entity.getMethod().name(), Entity.entity(entity.getEntity(), MediaType.APPLICATION_JSON_TYPE), responseType);
            }
            return invoker.method(entity.getMethod().name(), responseType);
        }
        return invoker.get(responseType);
    }

    @VisibleForTesting
    protected <T> Future<T> invokePreparedRequest(ClientEntity entity, GenericType<T> responseType, AsyncInvoker invoker)
    {
        if ( entity != null )
        {
            if ( entity.getEntity() != null )
            {
                return invoker.method(entity.getMethod().name(), Entity.entity(entity.getEntity(), MediaType.APPLICATION_JSON_TYPE), responseType);
            }
            return invoker.method(entity.getMethod().name(), responseType);
        }
        return invoker.get(responseType);
    }

    @VisibleForTesting
    protected WebTarget prepareTarget(ForPage forPage, ClientEntity entity, String[] additionalPaths)
    {
        WebTarget localTarget = target;
        if ( additionalPaths != null )
        {
            for ( String path : additionalPaths )
            {
                localTarget = localTarget.path(path);
            }
        }

        if ( forPage != null )
        {
            localTarget = localTarget.queryParam("page", forPage.getPage()).queryParam("per_page", forPage.getPerPage());
        }

        if ( (entity != null) && (entity.getQueryParams() != null) )
        {
            for ( Map.Entry<String, String> entry : entity.getQueryParams().entrySet() )
            {
                localTarget = localTarget.queryParam(entry.getKey(), entry.getValue());
            }
        }

        return localTarget;
    }

    @Override
    public void close()
    {
        if ( isClosed.compareAndSet(false, true) )
        {
            client.close();
        }
    }

    private AsyncInvoker prepareRequest(ForPage forPage, ClientEntity entity, String... additionalPaths)
    {
        WebTarget localTarget = prepareTarget(forPage, entity, additionalPaths);

        Invocation.Builder builder = localTarget.request().accept(MediaType.APPLICATION_JSON_TYPE);
        if ( configuration.getApiToken() != null )
        {
            builder = builder.header("X-AUTH-TOKEN", configuration.getApiToken());
        }

        return builder.async();
    }
}
