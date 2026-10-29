# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

XM Gate: API gateway of the XM Online platform. Spring Boot 4 + Spring Cloud Gateway **Server MVC** (servlet-based, not WebFlux), Java 25, Gradle wrapper. Most platform behavior (tenant context, config client, permissions, IDP, i18n, metrics) comes from `com.icthh.xm.commons:*` libraries (`xm_commons_version` in `gradle.properties`). Dependency versions live in `gradle.properties`.

## Commands

```bash
./gradlew bootRun                                                   # needs Consul + config server
./gradlew bootRun --args='--spring.profiles.active=dev,noconsul'    # static instances from application-noconsul.yml
./gradlew test                                                      # runs only *UnitTest* and *IntTest* classes
./gradlew test --tests 'com.icthh.xm.gate.gateway.filter.ApiPrefixIntTest'          # single class
./gradlew test --tests '*AccessControlAuthorizationManagerUnitTest.someMethodName'  # single method
./gradlew test -PremoteTc                                           # Testcontainers on remote host (remoteTc.host in ~/.gradle/gradle.properties)
./gradlew check                                                     # tests + PMD (qa/pmd.xml) + SpotBugs; Checkstyle is disabled
./gradlew -Pprod clean bootJar
./gradlew bootJar jibDockerBuild
```

Test naming matters: the `test` task includes only `**/*UnitTest*` and `**/*IntTest*`, so a test class with another suffix silently never runs. Integration tests use the `@IntegrationTest` meta-annotation (`GateApp` + `TestSecurityConfiguration` + `AsyncSyncConfiguration`) and run with the `test` profile (`src/test/resources/config/`).

## Architecture

### Request pipeline (order matters)

Servlet filters, ordered via constants in `config/Constants.java`:

1. `TenantInitFilter` (`HIGHEST_PRECEDENCE`): resolves tenant from `Host` via `TenantMappingService`, rejects unknown/suspended tenants with 400 (`SERVICE-NOT-FOUND` / `SERVICE-SUSPENDED`), sets and finally destroys `TenantContextHolder`. Default tenant is `XM`.
2. `RequestLoggingFilter` (`+1`): logs every request (replaced the old `AddHighLog`/`AddLogging` route filters).
3. `ApiPrefixStrippingFilter` (`+2`): strips optional `application.gateway.api-prefix` (default `/xm-api`). Everything after it (security matchers, access control, routing) assumes the **first path segment is the service name**.
4. Spring Security chain (`MicroserviceSecurityConfiguration`), then gateway routing.

### Security / authorization

`MicroserviceSecurityConfiguration` holds the ordered `requestMatchers` list: explicit deny (`/config/api/private/**`), `SUPER_ADMIN`-only paths (`/*/management/**`, `/management/**`), permit-all public/anonymous/oauth paths, then `anyRequest().access(AccessControlAuthorizationManager)`. Matcher order is security-relevant: recent commits tightened it, check `git log` before reordering.

`AccessControlAuthorizationManager` decision chain, first non-empty answer wins:
1. `TenantAuthorizationRule` beans (extension point, no implementation in this repo).
2. `application.gateway.auth-request-matcher-rules` (Ant path pattern, `permitAll`, `authorities`).
3. Service name from first path segment: in `application.gateway.xme-routes` → allow (downstream service enforces auth); otherwise requires authentication.

JWT is validated with an RSA public key fetched from the config server (`XmConfigServerService`) at startup. OAuth2 login against external IDPs is per tenant (`security/oauth2/idp`, `IdpConfigRepository`), configured via `idp-public.yml` / `idp-private.yml` in the config repo.

### Routing

Two route sources:
- Static YAML routes in `application-route.yml` (`spring.cloud.gateway.server.webmvc.routes`) with custom filters `TfaTokenDetection`, `AddDomainRelayHeaders`, `IdpStatefulMode`.
- `GatewayRoutesConfiguration` (profile `client-discovery`): builds a `/{serviceId}/**` route per Consul-discovered service, minus `application.gateway.excluded-services`.

Custom gateway filters live in `gateway/functions/*FilterFunctions`. To be usable from YAML, each needs a nested `FilterSupplier` registered as a bean in `GatewayHandlerFilterConfiguration`.

### Tenant-scoped runtime config

Classes implementing xm-commons refreshable config listeners (`TenantMappingService`, `TenantDomainRepository`, `TenantPropertiesService`, `IdpConfigRepository`, `DownloadFileConfigService`) load per-tenant YAML from the XM config server (`/config/tenants/{tenant}/...`) and are updated live through Kafka (`config_topic`). `listener/SystemTopicConsumer` consumes `application.kafka-system-topic` (default `system_topic`). Tests stub the config server via `TenantConfigMockConfiguration`.

### File endpoints

- `UploadResource`: `POST|PUT /upload/{service}/{path}` multipart proxy, deprecated and off unless `application.enable-upload-resource=true`.
- `DownloadResource`: `GET /api/download/{recordType}/{fileName}`, path resolved by per-tenant `file-download.yml` patterns (`service/file/download/strategy`), requires `RESOURCE.FILE.DOWNLOAD`.

## Notes

- Project version in `gradle.properties` is bumped automatically by a GitHub workflow on PRs to `main`; do not bump it manually.
