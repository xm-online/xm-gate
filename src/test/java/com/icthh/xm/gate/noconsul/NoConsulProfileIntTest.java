package com.icthh.xm.gate.noconsul;

import com.icthh.xm.gate.GateApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"noconsul"})
@SpringBootTest(classes = {GateApp.class, NoConsulTestConfiguration.class})
class NoConsulProfileIntTest {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ServiceInstanceChooser serviceInstanceChooser;

    static Stream<Arguments> services() {
        return Stream.of(
            Arguments.of("config", 8084),
            Arguments.of("uaa", 9999),
            Arguments.of("entity", 8081)
        );
    }

    @ParameterizedTest
    @MethodSource("services")
    void discoveryClientResolvesService(String name, int port) {
        List<ServiceInstance> instances = discoveryClient.getInstances(name);

        assertThat(instances).isNotEmpty();
        assertThat(instances.get(0).getHost()).isEqualTo("localhost");
        assertThat(instances.get(0).getPort()).isEqualTo(port);
    }

    @Test
    void discoveryClientReturnsAllConfiguredServices() {
        List<String> services = discoveryClient.getServices();
        assertThat(services).containsExactlyInAnyOrder("config", "uaa", "entity", "function");
    }

    @Test
    void discoveryClientReturnsEmptyListForUnknownService() {
        List<ServiceInstance> instances = discoveryClient.getInstances("unknown-service");
        assertThat(instances).isEmpty();
    }

    @Test
    void serviceInstanceChooserResolvesConfigService() {
        ServiceInstance configInstance = serviceInstanceChooser.choose("config");
        ServiceInstance uaaInstance = serviceInstanceChooser.choose("uaa");
        ServiceInstance entityInstance = serviceInstanceChooser.choose("entity");
        ServiceInstance unknownInstance = serviceInstanceChooser.choose("unknown-service");

        assertThat(configInstance).isNotNull();
        assertThat(uaaInstance).isNotNull();
        assertThat(entityInstance).isNotNull();
        assertThat(unknownInstance).isNull();

        assertThat(configInstance.getUri().toString()).isEqualTo("http://localhost:8084");
        assertThat(uaaInstance.getUri().toString()).isEqualTo("http://localhost:9999");
        assertThat(entityInstance.getUri().toString()).isEqualTo("http://localhost:8081");
    }
}
