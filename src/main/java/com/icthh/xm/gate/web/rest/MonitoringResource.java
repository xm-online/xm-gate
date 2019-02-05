package com.icthh.xm.gate.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.gate.web.rest.dto.Service;
import com.icthh.xm.gate.web.rest.dto.ServiceHealth;
import com.icthh.xm.gate.web.rest.dto.ServiceInstance;
import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;
import lombok.SneakyThrows;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringResource {

    @GetMapping("/services")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_LIST')")
    public ResponseEntity<List<Service>> getServices() {
        return ResponseEntity.ok(Arrays.asList(
            Service.builder()
                .name("gate")
                .instances(Arrays.asList(
                    ServiceInstance.builder()
                        .id("gate-58f9df805f14938cb3219e35f8a72182")
                        .address("10.0.0.1")
                        .port(8080)
                        .build(),
                    ServiceInstance.builder()
                        .id("gate-18f9df805f14938cb3219e35f8a72182")
                        .address("10.0.0.2")
                        .port(8081)
                        .build()))
                .build(),
            Service.builder()
                .name("uaa")
                .instances(Arrays.asList(
                    ServiceInstance.builder()
                        .id("uaa-58f9df805f14938cb3219e35f8a72182")
                        .address("10.0.1.1")
                        .port(9999)
                        .build(),
                    ServiceInstance.builder()
                        .id("uaa-18f9df805f14938cb3219e35f8a72182")
                        .address("10.0.1.2")
                        .port(9998)
                        .build()))
                .build()));
    }

    @SneakyThrows
    @GetMapping("/services/{serviceName}/health")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_HEALTH')")
    public ResponseEntity<List<ServiceHealth>> getHealth(@PathVariable String serviceName) {
        Health mock = Health
            .up()
            .withDetail("diskSpace", new HashMap<String, Object>() {{
                put("status", "UP");
                put("details", new HashMap<String, Object>() {{
                    put("total", "49243975680");
                    put("free", "5277057024");
                    put("threshold", "10485760");
                }});
            }})
            .build();

        return ResponseEntity.ok(Arrays.asList(
            ServiceHealth.builder()
                .instanceId("gate-58f9df805f14938cb3219e35f8a72182")
                .health(mock)
                .build(),
            ServiceHealth.builder()
                .instanceId("gate-18f9df805f14938cb3219e35f8a72182")
                .health(mock)
                .build()
        ));
    }

    @SneakyThrows
    @GetMapping("/services/{serviceName}/metrics")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_METRIC')")
    public ResponseEntity<List<ServiceMetrics>> getMetrics(@PathVariable String serviceName) {
        Map mock = new ObjectMapper().readValue("{\n" +
            "    \"version\": \"3.1.3\",\n" +
            "    \"gauges\": {\n" +
            "        \"HikariPool-1.pool.ActiveConnections\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"HikariPool-1.pool.IdleConnections\": {\n" +
            "            \"value\": 10\n" +
            "        },\n" +
            "        \"HikariPool-1.pool.PendingConnections\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"HikariPool-1.pool.TotalConnections\": {\n" +
            "            \"value\": 10\n" +
            "        },\n" +
            "        \"jvm.buffers.direct.capacity\": {\n" +
            "            \"value\": 2211957\n" +
            "        },\n" +
            "        \"jvm.buffers.direct.count\": {\n" +
            "            \"value\": 209\n" +
            "        },\n" +
            "        \"jvm.buffers.direct.used\": {\n" +
            "            \"value\": 2211957\n" +
            "        },\n" +
            "        \"jvm.buffers.mapped.capacity\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.buffers.mapped.count\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.buffers.mapped.used\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.files\": {\n" +
            "            \"value\": 0.0000858306884765625\n" +
            "        },\n" +
            "        \"jvm.garbage.PS-MarkSweep.count\": {\n" +
            "            \"value\": 12\n" +
            "        },\n" +
            "        \"jvm.garbage.PS-MarkSweep.time\": {\n" +
            "            \"value\": 2467\n" +
            "        },\n" +
            "        \"jvm.garbage.PS-Scavenge.count\": {\n" +
            "            \"value\": 155\n" +
            "        },\n" +
            "        \"jvm.garbage.PS-Scavenge.time\": {\n" +
            "            \"value\": 1448\n" +
            "        },\n" +
            "        \"jvm.memory.heap.committed\": {\n" +
            "            \"value\": 525336576\n" +
            "        },\n" +
            "        \"jvm.memory.heap.init\": {\n" +
            "            \"value\": 262144000\n" +
            "        },\n" +
            "        \"jvm.memory.heap.max\": {\n" +
            "            \"value\": 525336576\n" +
            "        },\n" +
            "        \"jvm.memory.heap.usage\": {\n" +
            "            \"value\": 0.34224947626719215\n" +
            "        },\n" +
            "        \"jvm.memory.heap.used\": {\n" +
            "            \"value\": 179796168\n" +
            "        },\n" +
            "        \"jvm.memory.non-heap.committed\": {\n" +
            "            \"value\": 174194688\n" +
            "        },\n" +
            "        \"jvm.memory.non-heap.init\": {\n" +
            "            \"value\": 2555904\n" +
            "        },\n" +
            "        \"jvm.memory.non-heap.max\": {\n" +
            "            \"value\": -1\n" +
            "        },\n" +
            "        \"jvm.memory.non-heap.usage\": {\n" +
            "            \"value\": -168624184\n" +
            "        },\n" +
            "        \"jvm.memory.non-heap.used\": {\n" +
            "            \"value\": 168624248\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Code-Cache.committed\": {\n" +
            "            \"value\": 46006272\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Code-Cache.init\": {\n" +
            "            \"value\": 2555904\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Code-Cache.max\": {\n" +
            "            \"value\": 251658240\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Code-Cache.usage\": {\n" +
            "            \"value\": 0.18157450358072916\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Code-Cache.used\": {\n" +
            "            \"value\": 45694720\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Compressed-Class-Space.committed\": {\n" +
            "            \"value\": 14548992\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Compressed-Class-Space.init\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Compressed-Class-Space.max\": {\n" +
            "            \"value\": 1073741824\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Compressed-Class-Space.usage\": {\n" +
            "            \"value\": 0.012785643339157104\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Compressed-Class-Space.used\": {\n" +
            "            \"value\": 13728480\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Metaspace.committed\": {\n" +
            "            \"value\": 113639424\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Metaspace.init\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Metaspace.max\": {\n" +
            "            \"value\": -1\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Metaspace.usage\": {\n" +
            "            \"value\": 0.96094644055922\n" +
            "        },\n" +
            "        \"jvm.memory.pools.Metaspace.used\": {\n" +
            "            \"value\": 109201400\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Eden-Space.committed\": {\n" +
            "            \"value\": 157286400\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Eden-Space.init\": {\n" +
            "            \"value\": 66060288\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Eden-Space.max\": {\n" +
            "            \"value\": 157810688\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Eden-Space.usage\": {\n" +
            "            \"value\": 0.6090987196000311\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Eden-Space.used\": {\n" +
            "            \"value\": 96122288\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Eden-Space.used-after-gc\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Old-Gen.committed\": {\n" +
            "            \"value\": 358088704\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Old-Gen.init\": {\n" +
            "            \"value\": 175112192\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Old-Gen.max\": {\n" +
            "            \"value\": 358088704\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Old-Gen.usage\": {\n" +
            "            \"value\": 0.2141438340372781\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Old-Gen.used\": {\n" +
            "            \"value\": 76682488\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Old-Gen.used-after-gc\": {\n" +
            "            \"value\": 76666104\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Survivor-Space.committed\": {\n" +
            "            \"value\": 9961472\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Survivor-Space.init\": {\n" +
            "            \"value\": 10485760\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Survivor-Space.max\": {\n" +
            "            \"value\": 9961472\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Survivor-Space.usage\": {\n" +
            "            \"value\": 0.7164836682771382\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Survivor-Space.used\": {\n" +
            "            \"value\": 7137232\n" +
            "        },\n" +
            "        \"jvm.memory.pools.PS-Survivor-Space.used-after-gc\": {\n" +
            "            \"value\": 7137232\n" +
            "        },\n" +
            "        \"jvm.memory.total.committed\": {\n" +
            "            \"value\": 699531264\n" +
            "        },\n" +
            "        \"jvm.memory.total.init\": {\n" +
            "            \"value\": 264699904\n" +
            "        },\n" +
            "        \"jvm.memory.total.max\": {\n" +
            "            \"value\": 525336575\n" +
            "        },\n" +
            "        \"jvm.memory.total.used\": {\n" +
            "            \"value\": 348572496\n" +
            "        },\n" +
            "        \"jvm.threads.blocked.count\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.threads.count\": {\n" +
            "            \"value\": 91\n" +
            "        },\n" +
            "        \"jvm.threads.daemon.count\": {\n" +
            "            \"value\": 13\n" +
            "        },\n" +
            "        \"jvm.threads.deadlock.count\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.threads.deadlocks\": {\n" +
            "            \"value\": []\n" +
            "        },\n" +
            "        \"jvm.threads.new.count\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.threads.runnable.count\": {\n" +
            "            \"value\": 16\n" +
            "        },\n" +
            "        \"jvm.threads.terminated.count\": {\n" +
            "            \"value\": 0\n" +
            "        },\n" +
            "        \"jvm.threads.timed_waiting.count\": {\n" +
            "            \"value\": 6\n" +
            "        },\n" +
            "        \"jvm.threads.waiting.count\": {\n" +
            "            \"value\": 69\n" +
            "        }\n" +
            "    },\n" +
            "    \"counters\": {\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.activeRequests\": {\n" +
            "            \"count\": 1\n" +
            "        }\n" +
            "    },\n" +
            "    \"histograms\": {\n" +
            "        \"HikariPool-1.pool.ConnectionCreation\": {\n" +
            "            \"count\": 19,\n" +
            "            \"max\": 19,\n" +
            "            \"mean\": 4.885607033066878,\n" +
            "            \"min\": 2,\n" +
            "            \"p50\": 6,\n" +
            "            \"p75\": 7,\n" +
            "            \"p95\": 8,\n" +
            "            \"p98\": 8,\n" +
            "            \"p99\": 8,\n" +
            "            \"p999\": 8,\n" +
            "            \"stddev\": 2.5600851443588555\n" +
            "        },\n" +
            "        \"HikariPool-1.pool.Usage\": {\n" +
            "            \"count\": 564,\n" +
            "            \"max\": 2058,\n" +
            "            \"mean\": 0.6214669220019793,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 8,\n" +
            "            \"p98\": 8,\n" +
            "            \"p99\": 8,\n" +
            "            \"p999\": 8,\n" +
            "            \"stddev\": 1.91366639037328\n" +
            "        }\n" +
            "    },\n" +
            "    \"meters\": {\n" +
            "        \"HikariPool-1.pool.ConnectionTimeoutRate\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.errors\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.badRequest\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.created\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.noContent\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.notFound\": {\n" +
            "            \"count\": 1,\n" +
            "            \"m15_rate\": 0.0011080303990206543,\n" +
            "            \"m1_rate\": 0.015991117074135343,\n" +
            "            \"m5_rate\": 0.0033057092356765017,\n" +
            "            \"mean_rate\": 0.0003657067187603609,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.ok\": {\n" +
            "            \"count\": 276,\n" +
            "            \"m15_rate\": 0.10495260662232087,\n" +
            "            \"m1_rate\": 0.09583574305049128,\n" +
            "            \"m5_rate\": 0.09925834945585314,\n" +
            "            \"mean_rate\": 0.10093504371538987,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.other\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.responseCodes.serverError\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.timeouts\": {\n" +
            "            \"count\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"units\": \"events/second\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"timers\": {\n" +
            "        \"HikariPool-1.pool.Wait\": {\n" +
            "            \"count\": 564,\n" +
            "            \"max\": 0.0036211700000000004,\n" +
            "            \"mean\": 0.00016249235014213626,\n" +
            "            \"min\": 3.7400000000000004e-7,\n" +
            "            \"p50\": 0.000002899,\n" +
            "            \"p75\": 0.000343222,\n" +
            "            \"p95\": 0.0005085760000000001,\n" +
            "            \"p98\": 0.0005085760000000001,\n" +
            "            \"p99\": 0.0005085760000000001,\n" +
            "            \"p999\": 0.0005236990000000001,\n" +
            "            \"stddev\": 0.0001827619757909963,\n" +
            "            \"m15_rate\": 0.23940826304632945,\n" +
            "            \"m1_rate\": 0.20766260330015773,\n" +
            "            \"m5_rate\": 0.2019636929337708,\n" +
            "            \"mean_rate\": 0.20439535394310032,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.codahale.metrics.servlet.InstrumentedFilter.requests\": {\n" +
            "            \"count\": 277,\n" +
            "            \"max\": 1.092526389,\n" +
            "            \"mean\": 0.009169057246646623,\n" +
            "            \"min\": 0.0035096130000000004,\n" +
            "            \"p50\": 0.008364826,\n" +
            "            \"p75\": 0.009490651000000001,\n" +
            "            \"p95\": 0.020591427000000002,\n" +
            "            \"p98\": 0.020591427000000002,\n" +
            "            \"p99\": 0.020591427000000002,\n" +
            "            \"p999\": 0.020591427000000002,\n" +
            "            \"stddev\": 0.004425499872910093,\n" +
            "            \"m15_rate\": 0.10606063702134153,\n" +
            "            \"m1_rate\": 0.11182686012462663,\n" +
            "            \"m5_rate\": 0.10256405869152965,\n" +
            "            \"mean_rate\": 0.10130073586834537,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.activateAccount\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.changePassword\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.checkPasswordReset\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.disableTwoFactorAuth\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.enableTwoFactorAuth\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.finishPasswordReset\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.getAccount\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.getTfaAvailableOtpChannelSpecs\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.isAuthenticated\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.isCaptchaNeed\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.registerAccount\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.requestPasswordReset\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.resetActivationKey\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.saveAccount\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.AccountResource.updateUserLogins\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.ClientResource.createClient\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.ClientResource.deleteClient\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.ClientResource.getAllClients\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.ClientResource.getClient\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.ClientResource.updateClient\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.OnlineUsersResource.getUsersOnline\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.RoleResource.createRole\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.RoleResource.deleteUser\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.RoleResource.getRole\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.RoleResource.getRoleMatrix\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.RoleResource.getRoles\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.RoleResource.updateRole\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.RoleResource.updateRoleMatrix\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.TenantLoginsResource.getLogins\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.TenantLoginsResource.updateLogins\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.TenantLoginsResource.validate\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.TenantPropertiesResource.updateTenantProperties\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.TenantPropertiesResource.validate\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.createUser\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.deleteUser\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.disableTwoFactorAuth\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.enableTwoFactorAuth\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.getAllUsers\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.getPublicUser\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.getTfaAvailableOtpChannelSpecs\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.getUser\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.getUserByLogin\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.updateUser\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        },\n" +
            "        \"com.icthh.xm.uaa.web.rest.UserResource.updateUserLogins\": {\n" +
            "            \"count\": 0,\n" +
            "            \"max\": 0,\n" +
            "            \"mean\": 0,\n" +
            "            \"min\": 0,\n" +
            "            \"p50\": 0,\n" +
            "            \"p75\": 0,\n" +
            "            \"p95\": 0,\n" +
            "            \"p98\": 0,\n" +
            "            \"p99\": 0,\n" +
            "            \"p999\": 0,\n" +
            "            \"stddev\": 0,\n" +
            "            \"m15_rate\": 0,\n" +
            "            \"m1_rate\": 0,\n" +
            "            \"m5_rate\": 0,\n" +
            "            \"mean_rate\": 0,\n" +
            "            \"duration_units\": \"seconds\",\n" +
            "            \"rate_units\": \"calls/second\"\n" +
            "        }\n" +
            "    }\n" +
            "}", Map.class);

        return ResponseEntity.ok(Arrays.asList(
            ServiceMetrics.builder()
                .instanceId("gate-58f9df805f14938cb3219e35f8a72182")
                .metrics(mock)
                .build(),
            ServiceMetrics.builder()
                .instanceId("gate-18f9df805f14938cb3219e35f8a72182")
                .metrics(mock)
                .build()
        ));
    }
}
