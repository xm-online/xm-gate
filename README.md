# XM Gate

XM Gate is an API Gateway microservice built with Spring Boot 4.1 and Spring Cloud Gateway Server MVC (servlet stack). It serves as the central entry point for the XM Online microservices architecture, providing routing, security, multi-tenancy support, and file handling capabilities.

## Table of Contents

- [Technology Stack](#technology-stack)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running with Consul](#running-with-consul)
  - [Running without Consul (No-Consul Mode)](#running-without-consul-no-consul-mode)
- [Routes Configuration](#routes-configuration)
  - [Route Definition](#route-definition)
  - [Available Gateway Filters](#available-gateway-filters)
- [File Upload](#file-upload)
- [File Download](#file-download)
  - [Download Configuration](#download-configuration)
  - [Path Pattern Strategies](#path-pattern-strategies)
- [Identity Provider (IDP) Configuration](#identity-provider-idp-configuration)
- [Multi-Tenancy](#multi-tenancy)
- [Access Control](#access-control)
- [Application Properties](#application-properties)
- [Building for Production](#building-for-production)
- [Testing](#testing)
- [Docker Support](#docker-support)
- [Code Quality](#code-quality)

## Technology Stack

| Technology | Version |
|------------|---------|
| Java | 25 |
| Spring Boot | 4.1.0 |
| Spring Cloud | 2025.1.3 |
| Spring Cloud Gateway | Server MVC (managed by Spring Cloud BOM) |
| XM Commons | 5.0.46 |
| JHipster | 9.0.0-beta.0 |
| Gradle (wrapper) | 9.2.1 |

Exact versions are defined in `gradle.properties`.

## Features

- **API Gateway** - Routes requests to backend microservices
- **Multi-tenancy** - Domain-based tenant resolution
- **Service Discovery** - Consul integration with no-consul fallback mode
- **API Prefix** - Optional public prefix (default `/xm-api`) stripped before security and routing
- **File Upload** - Multipart file upload proxy to microservices (deprecated, disabled by default)
- **File Download** - Secure file download with configurable path patterns
- **IDP Integration** - External Identity Provider support (OAuth2/OIDC)
- **Access Control** - Path-based authorization rules
- **Observability** - Metrics, tracing, request logging and Logstash support

## Getting Started

### Prerequisites

- Java 25
- Gradle wrapper (`./gradlew`)
- Consul (optional, for service discovery)
- Keycloak or other OIDC provider (for authentication)

### Running with Consul

By default, the application requires Consul for service discovery and configuration.

1. Start Consul:
```bash
docker compose -f src/main/docker/consul.yml up -d
```

2. Run the application:
```bash
./gradlew bootRun
```

### Running without Consul (No-Consul Mode)

The application can run without Consul by using the `noconsul` profile. This is useful for local development or environments where Consul is not available.

1. Activate the `noconsul` profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev,noconsul'
```

Or set the environment variable:
```bash
SPRING_PROFILES_ACTIVE=dev,noconsul ./gradlew bootRun
```

2. Configure service instances in `application-noconsul.yml`:
```yaml
spring:
  cloud:
    consul:
      enabled: false
      config:
        enabled: false
      discovery:
        enabled: false
    loadbalancer:
      enabled: true
    discovery:
      enabled: true
      client:
        simple:
          instances:
            config:
              - uri: http://localhost:8084
            uaa:
              - uri: http://localhost:9999
            entity:
              - uri: http://localhost:8081
```

The no-consul mode uses Spring Cloud's simple discovery client to define static service instances.

## Routes Configuration

Routes come from two sources:

- **Static routes** in `application-route.yml` (Spring Cloud Gateway Server MVC YAML configuration).
- **Discovered routes** (profile `client-discovery`): `GatewayRoutesConfiguration` creates a `/{serviceId}/**` route for every service registered in Consul, except `application.gateway.excluded-services`. These routes apply `StripPrefix=1`, `TfaTokenDetection`, `AddDomainRelayHeaders` and `IdpStatefulMode`.

Current routes are available at `GET /api/gateway/routes`.

### Route Definition

Routes are defined under `spring.cloud.gateway.server.webmvc.routes`:

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: entity
              uri: lb://entity
              predicates:
                - Path=/entity/**
              filters:
                - StripPrefix=1
                - TfaTokenDetection
                - AddDomainRelayHeaders
                - IdpStatefulMode
                - AddRequestHeader=X-Request-service, [ENTITY]
```

**Route Properties:**
- `id` - Unique identifier for the route
- `uri` - Target service URI (use `lb://` prefix for load-balanced services)
- `predicates` - Conditions for matching requests (e.g., `Path`, `Method`, `Header`)
- `filters` - Request/response transformations

### Available Gateway Filters

Custom XM filters (registered in `GatewayHandlerFilterConfiguration`):

| Filter | Description |
|--------|-------------|
| `AddDomainRelayHeaders` | Adds tenant context headers (`x-domain`, `x-tenant`, `x-scheme`, `x-port`, `x-webapp-url`) |
| `IdpStatefulMode` | Handles IDP stateful authentication mode |
| `TfaTokenDetection` | Detects two-factor authentication tokens |

Standard Spring Cloud Gateway filters such as `StripPrefix=N` and `AddRequestHeader` are available as well.

Tenant initialization, request logging and access control are not route filters: they run for every request as servlet filters / Spring Security (see [Multi-Tenancy](#multi-tenancy) and [Access Control](#access-control)). The former `TenantInit`, `AccessControl`, `AddHighLog` and `AddLogging` route filters no longer exist.

**Excluding Services from Routing:**

Configure services to exclude from dynamic routing:
```yaml
application:
  gateway:
    excluded-services:
      - consul
      - gate
```

## File Upload

> **Deprecated, disabled by default.** Enable with `application.enable-upload-resource: true`. It also requires `spring.servlet.multipart.enabled: true`, because gateway webmvc disables multipart parsing otherwise.

XM Gate provides a dedicated endpoint for proxying multipart file uploads to backend microservices.

**Endpoint:** `POST /upload/{service}/{path}`

**Example:**
```bash
curl -X POST \
  http://localhost:8080/upload/entity/api/functions/UPLOAD/execute \
  -F "file=@document.pdf"
```

This request proxies the multipart form to the `entity` service at `/api/functions/UPLOAD/execute`.

**Features:**
- Streams files without loading into memory
- Supports POST and PUT methods
- Preserves all form parameters and headers
- Validates service names (alphanumeric only)
- Security validation to prevent path traversal

**Implementation:** `com.icthh.xm.gate.web.rest.file.UploadResource`

## File Download

XM Gate supports secure file downloads with configurable path resolution strategies.

**Endpoint:** `GET /api/download/{recordType}/{fileName}`

**Example:**
```bash
curl -X GET http://localhost:8080/api/download/reports/monthly-report.pdf \
  -H "Authorization: Bearer <token>"
```

**Security:** Requires `RESOURCE.FILE.DOWNLOAD` permission.

### Download Configuration

Configure download patterns per tenant in `/config/tenants/{tenantName}/file-download.yml`:

```yaml
patterns:
  - key: date
    strategy: XM_TOKEN_MATCHER
    pathPrefix: /files/{yyyy}/{mm}/{dd}/
  - key: token
    strategy: XM_TOKEN_MATCHER
    pathPrefix: /{tenant}/{userKey}/{additionalDetails.testParam1}/
  - key: mixed
    strategy: XM_TOKEN_MATCHER
    pathPrefix: /root/{yyyy}/{tenant}/{mm}/{userKey}/
  - key: const
    strategy: XM_TOKEN_MATCHER
    pathPrefix: /static/media/
```

**Configuration Properties:**
- `key` - Unique identifier used as `recordType` in the download URL
- `strategy` - Path resolution strategy (`XM_TOKEN_MATCHER`)
- `pathPrefix` - Template for file path resolution

### Path Pattern Strategies

The `XM_TOKEN_MATCHER` strategy supports the following placeholders:
- `{yyyy}`, `{mm}`, `{dd}` - Date components
- `{tenant}` - Current tenant key
- `{userKey}` - User identifier from JWT token
- `{additionalDetails.*}` - Custom claims from JWT token

**File Storage Root:**

Configure the base path for file storage:
```yaml
application:
  object-storage-file-root: /var/data/files
```

**Implementation:** `com.icthh.xm.gate.web.rest.file.DownloadResource`

## Identity Provider (IDP) Configuration

XM Gate supports external Identity Providers through OAuth2/OIDC. IDP configuration is tenant-specific and loaded from the config server.

**Public Configuration:** `/config/tenants/{tenant}/idp-public.yml`
```yaml
idp:
  config:
    features:
      # Feature flags for IDP behavior
    clients:
      - key: google
        name: Google
        clientId: your-client-id
        redirectUri: "{baseUrl}/login/oauth2/code/{registrationId}"
        openIdConfig:
          authorizationEndpoint:
            uri: https://accounts.google.com/o/oauth2/v2/auth
          tokenEndpoint:
            uri: https://oauth2.googleapis.com/token
            grantType: authorization_code
          userinfoEndpoint:
            uri: https://openidconnect.googleapis.com/v1/userinfo
            userNameAttributeName: email
          jwksEndpoint:
            uri: https://www.googleapis.com/oauth2/v3/certs
```

**Private Configuration:** `/config/tenants/{tenant}/idp-private.yml`
```yaml
idp:
  config:
    clients:
      - key: google
        clientSecret: your-client-secret
        scope:
          - openid
          - profile
          - email
```

## Multi-Tenancy

XM Gate provides domain-based multi-tenancy. Tenants are resolved from the request domain.

**Tenant Resolution Flow** (`TenantInitFilter`, runs first for every request):
1. Extract domain from `Host` header
2. Look up tenant mapping in configuration (unmapped domains resolve to the default tenant `XM`)
3. Reject unknown tenants with `400 {"error": "SERVICE-NOT-FOUND"}` and inactive ones with `400 {"error": "SERVICE-SUSPENDED"}`
4. Set tenant context for the request
5. `AddDomainRelayHeaders` route filter adds tenant headers to downstream requests

**Tenant Management API:** `/api/tenants` (create, list, get, update state, delete), protected by `GATE.TENANT.*` permissions.

**Configure Host-to-Tenant Mapping:**
```yaml
application:
  hosts:
    - localhost
    - local
```

**Tenant Context Headers:**

| Header | Description |
|--------|-------------|
| `x-tenant` | Resolved tenant key |
| `x-domain` | Request domain |
| `x-scheme` | Request scheme (http/https) |
| `x-port` | Request port |
| `x-webapp-url` | Referer-based webapp URL |

## Access Control

Authorization is configured in `MicroserviceSecurityConfiguration`. Matchers are evaluated in order:

| Path | Access |
|------|--------|
| `/config/api/private/**` | denied |
| `/*/management/**` | `SUPER_ADMIN` |
| `/*/api/public/**`, paths containing `/anonymous` | permit all |
| `/oauth2/authorization/**`, `/login/oauth2/code/**` | permit all |
| `/api/**` (gate's own API) | authenticated |
| `/*/public/**`, `/*/oauth/**` | permit all |
| `/management/health`, `/management/prometheus/**` | permit all |
| `/management/**` | `SUPER_ADMIN` |
| any other request | `AccessControlAuthorizationManager` |

`AccessControlAuthorizationManager` decides in this order, the first rule that matches wins:
1. `TenantAuthorizationRule` beans (extension point).
2. `application.gateway.auth-request-matcher-rules`:
   ```yaml
   application:
     gateway:
       auth-request-matcher-rules:
         - path-pattern: /entity/api/reports/**
           authorities: [ROLE_ADMIN]
         - path-pattern: /entity/api/open/**
           permit-all: true
   ```
3. Service name (first path segment) listed in `application.gateway.xme-routes` is allowed and the downstream service enforces security. Any other service requires an authenticated user.

## API Prefix

`ApiPrefixStrippingFilter` strips `application.gateway.api-prefix` (default `/xm-api`) before security and routing, so `/xm-api/uaa/oauth/token` and `/uaa/oauth/token` are served the same way. Requests without the prefix pass unchanged.

## Application Properties

Key application properties in `application.yml`:

```yaml
application:
  # Hosts for tenant resolution
  hosts:
    - localhost
    - local

  # Kafka configuration
  kafka-enabled: true
  kafka-system-topic: system_topic

  # Retry configuration
  retry:
    max-attempts: 3
    delay: 1000
    multiplier: 2

  # Tenant properties
  tenant-properties-path-pattern: /config/tenants/{tenantName}/gate/${application.tenant-properties-name}
  tenant-properties-name: gate.yml
  tenant-ignored-path-list: /v2/api-docs, /v3/api-docs, /api/profile-info, /management/health

  # File storage
  object-storage-file-root: /

  # IDP settings
  disable-idp-cookie-usage: false
  redirect-to-default-tenant-enabled: false

  # Deprecated multipart upload proxy
  enable-upload-resource: false

  # Gateway settings
  gateway:
    api-prefix: /xm-api
    xme-routes: [config, uaa, entity, dashboard, scheduler, activation, timeline, search, websocket, function, webhook, communication]
    excluded-services:
      - consul
      - gate
    auth-request-matcher-rules: []

  # Outgoing HTTP client (timeouts in seconds)
  http-client:
    max-connections: 1000
    max-connections-per-route: 500
    connection-timeout-seconds: 21
    response-timeout-seconds: 66
    follow-redirects: false      # true: legacy behavior, gate follows upstream redirects
    decompress-responses: false  # true: legacy behavior, gate decompresses upstream responses

# XM Config client
xm-config:
  enabled: true
  xm-config-url: http://config:8084
  kafka-config-topic: config_topic

```

## Building for Production

### Package as JAR

```bash
./gradlew -Pprod clean bootJar
```

Run the JAR:
```bash
java -jar build/libs/*.jar
```

### Package as WAR

```bash
./gradlew -Pprod -Pwar clean bootWar
```

## Testing

Run all tests:
```bash
./gradlew test
```

Only classes matching `*UnitTest*` or `*IntTest*` are executed.

Run a single test class:
```bash
./gradlew test --tests 'com.icthh.xm.gate.gateway.filter.ApiPrefixIntTest'
```

Run unit tests only:
```bash
./gradlew test --tests '*UnitTest*'
```

Run integration tests only:
```bash
./gradlew test --tests '*IntTest*'
```

Run with test coverage:
```bash
./gradlew test jacocoTestReport
```

Run test against remote Testcontainer (com.xmedigital.gradle.remotetc).
Before start, add ```remoteTc.host=ssh://user@host in ~/.gradle/gradle.properties```
```bash
./gradlew test -PremoteTc
```

## Docker Support

### Build Docker Image

```bash
./gradlew bootJar jibDockerBuild
```

Or using npm:
```bash
npm run java:docker
```

For ARM64 (Apple Silicon):
```bash
npm run java:docker:arm64
```

### Run with Docker Compose

```bash
docker compose -f src/main/docker/app.yml up -d
```

### Available Docker Compose Files

| File | Description |
|------|-------------|
| `app.yml` | Main application |
| `services.yml` | Dependencies for local development |
| `consul.yml` | Consul service discovery |
| `monitoring.yml` | Prometheus + Grafana |
| `sonar.yml` | SonarQube code analysis |
| `zipkin.yml` | Distributed tracing |

## Code Quality

### SonarQube Analysis

Start SonarQube:
```bash
docker compose -f src/main/docker/sonar.yml up -d
```

Run analysis:
```bash
./gradlew -Pprod clean check jacocoTestReport sonarqube
```

### Static Analysis Tools

- **PMD** - Static code analysis (`qa/pmd.xml`)
- **SpotBugs** - Bug pattern detection
- **Checkstyle** - configured (`qa/checkstyle.xml`) but currently disabled in `build.gradle`

Run all checks:
```bash
./gradlew check
```

## License

This project is part of XM Online platform.

## Links

- [JHipster Documentation](https://www.jhipster.tech/documentation-archive/v9.0.0-beta.0)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/)
