package com.icthh.xm.gate.config.cassandra;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import com.datastax.driver.core.policies.RetryPolicy;
import com.datastax.driver.extras.codecs.jdk8.LocalDateCodec;
import io.github.jhipster.config.JHipsterConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty("jhipster.gateway.rate-limiting.enabled")
@EnableConfigurationProperties(CassandraProperties.class)
@Profile({JHipsterConstants.SPRING_PROFILE_DEVELOPMENT, JHipsterConstants.SPRING_PROFILE_PRODUCTION})
public class CassandraConfiguration {

    @Value("${spring.data.cassandra.protocolVersion:V4}")
    private ProtocolVersion protocolVersion;

    @Autowired(required = false)
    MetricRegistry metricRegistry;

    @Bean
    public Cluster cluster(CassandraProperties properties) {
        Cluster.Builder builder = Cluster.builder()
            .withClusterName(properties.getClusterName())
            .withProtocolVersion(protocolVersion)
            .withPort(getPort(properties));

        if (properties.getUsername() != null) {
            builder.withCredentials(properties.getUsername(), properties.getPassword());
        }
        if (properties.getCompression() != null) {
            builder.withCompression(properties.getCompression());
        }
        if (properties.getLoadBalancingPolicy() != null) {
            LoadBalancingPolicy policy = instantiate(properties.getLoadBalancingPolicy());
            builder.withLoadBalancingPolicy(policy);
        }
        builder.withQueryOptions(getQueryOptions(properties));
        if (properties.getReconnectionPolicy() != null) {
            ReconnectionPolicy policy = instantiate(properties.getReconnectionPolicy());
            builder.withReconnectionPolicy(policy);
        }
        if (properties.getRetryPolicy() != null) {
            RetryPolicy policy = instantiate(properties.getRetryPolicy());
            builder.withRetryPolicy(policy);
        }
        builder.withSocketOptions(getSocketOptions(properties));
        if (properties.isSsl()) {
            builder.withSSL();
        }
        List<String> contactPoints = properties.getContactPoints();
        builder.addContactPoints(contactPoints.toArray(new String[0]));

        Cluster cluster = builder.build();

        cluster.getConfiguration().getCodecRegistry()
            .register(LocalDateCodec.instance)
            .register(CustomZonedDateTimeCodec.instance);

        if (metricRegistry != null) {
            cluster.init();
            metricRegistry.registerAll(cluster.getMetrics().getRegistry());
        }

        return cluster;
    }

    protected int getPort(CassandraProperties properties) {
        return properties.getPort();
    }

    public static <T> T instantiate(Class<T> type) {
        return BeanUtils.instantiate(type);
    }

    private static QueryOptions getQueryOptions(CassandraProperties properties) {
        QueryOptions options = new QueryOptions();
        if (properties.getConsistencyLevel() != null) {
            options.setConsistencyLevel(properties.getConsistencyLevel());
        }
        if (properties.getSerialConsistencyLevel() != null) {
            options.setSerialConsistencyLevel(properties.getSerialConsistencyLevel());
        }
        options.setFetchSize(properties.getFetchSize());
        return options;
    }

    private static SocketOptions getSocketOptions(CassandraProperties properties) {
        SocketOptions options = new SocketOptions();
        options.setConnectTimeoutMillis((int) properties.getConnectTimeout().toMillis());
        options.setReadTimeoutMillis((int) properties.getReadTimeout().toMillis());
        return options;
    }

    @Bean(destroyMethod = "close")
    public Session session(CassandraProperties properties, Cluster cluster) {
        log.debug("Configuring Cassandra session");
        return StringUtils.hasText(properties.getKeyspaceName()) ? cluster.connect(properties.getKeyspaceName())
            : cluster.connect();
    }
}
