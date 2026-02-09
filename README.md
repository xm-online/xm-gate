# XM Gate

XM Gate is an API Gateway microservice built with Spring Boot 4.0.1 and Spring Cloud Gateway Server MVC. It serves as the central entry point for the XM Online microservices architecture, providing routing, security, multi-tenancy support, and file handling capabilities.

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
- [Rate Limiting](#rate-limiting)
- [Application Properties](#application-properties)
- [Building for Production](#building-for-production)
- [Testing](#testing)
- [Docker Support](#docker-support)
- [Code Quality](#code-quality)

## Technology Stack

| Technology | Version |
|------------|---------|
| Java | 25 |
| Spring Boot | 4.0.1 |
| Spring Cloud | 2025.1.0 |
| Spring Cloud Gateway Server MVC | 5.0.0 |
| JHipster | 9.0.0-beta.0 |
| Gradle | 9.1.0 |

## Features

- **API Gateway** - Routes requests to backend microservices
- **Multi-tenancy** - Domain-based tenant resolution
- **Service Discovery** - Consul integration with no-consul fallback mode
- **File Upload** - Multipart file upload proxy to microservices
- **File Download** - Secure file download with configurable path patterns
- **IDP Integration** - External Identity Provider support (OAuth2/OIDC)
- **Access Control** - Endpoint-level authorization
- **Rate Limiting** - Request rate limiting with Bucket4j
- **Observability** - Metrics, tracing, and logging support

## Getting Started

### Prerequisites

- Java 25
- Gradle 9.1.0 or use the Gradle wrapper (`./gradlew`)
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

Gateway routes are configured in `application-route.yml` using Spring Cloud Gateway Server MVC YAML configuration.

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
                - AddDomainRelayHeaders
                - AddHighLog
                - TenantInit
                - AccessControl
                - AddRequestHeader=X-Request-service, [ENTITY]
```

**Route Properties:**
- `id` - Unique identifier for the route
- `uri` - Target service URI (use `lb://` prefix for load-balanced services)
- `predicates` - Conditions for matching requests (e.g., `Path`, `Method`, `Header`)
- `filters` - Request/response transformations

### Available Gateway Filters

| Filter | Description |
|--------|-------------|
| `StripPrefix=N` | Removes N path segments from the request |
| `AddDomainRelayHeaders` | Adds tenant context headers (`x-domain`, `x-tenant`, `x-scheme`, `x-port`, `x-webapp-url`) |
| `AddHighLog` | Enables detailed logging for requests |
| `TenantInit` | Initializes tenant context from domain mapping |
| `AccessControl` | Enforces endpoint-level access control |
| `AddLogging` | Adds request/response logging |
| `IdpStatefulMode` | Handles IDP stateful authentication mode |
| `TfaTokenDetection` | Detects two-factor authentication tokens |
| `AddRequestHeader` | Adds custom headers to requests |
| `RateLimitByTenantKey` | Rate limits requests per tenant |
| `RateLimitByClientKey` | Rate limits requests per tenant + client ID |
| `RateLimitBySessionKey` | Rate limits requests per service + session ID |

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

XM Gate provides a dedicated endpoint for proxying multipart file uploads to backend microservices. This avoids memory issues with large file uploads by streaming files directly.

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

**Tenant Resolution Flow:**
1. Extract domain from `Host` header
2. Look up tenant mapping in configuration
3. Set tenant context for the request
4. Add `x-tenant` header to downstream requests

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

## Rate Limiting

XM Gate provides request rate limiting using [Bucket4j](https://bucket4j.com/) with Caffeine cache as the backend. 
Rate limiting helps protect backend services from excessive requests by throttling traffic based on configurable keys.

### Rate Limiting Filters

Three rate limiting filters are available for use in route configurations:

| Filter | Key Resolution | Description |
|--------|----------------|-------------|
| `RateLimitByTenantKey` | Tenant header (`x-tenant`) | Limits requests per tenant |
| `RateLimitByClientKey` | Tenant + Client ID from JWT | Limits requests per authenticated client within a tenant |
| `RateLimitBySessionKey` | Service name + Session ID | Limits requests per session for a specific service |

### Configuration

Rate limiting filters are configured per route in `application-route.yml`:

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: config
              uri: lb://config
              predicates:
                - Path=/config/**
              filters:
                - StripPrefix=1
                - name: RateLimitByTenantKey
                  args:
                    capacity: 10
                    periodInMinutes: 1
                - name: RateLimitByClientKey
                  args:
                    capacity: 5
                    periodInMinutes: 1
```

**Filter Parameters:**
- `capacity` - Maximum number of requests allowed in the time period (token bucket capacity)
- `periodInMinutes` - Time window in minutes for the rate limit

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
  kafka-system-queue: system_queue

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

  # Gateway settings
  gateway:
    excluded-services:
      - consul
      - gate

# XM Config client
xm-config:
  enabled: true
  xm-config-url: http://config:8084
  kafka-config-topic: config_topic

# JHipster settings
jhipster:
  gateway:
    rate-limiting:
      enabled: false
    authorized-microservices-endpoints:
      entity: /api/**
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
| `consul.yml` | Consul service discovery |
| `keycloak.yml` | Keycloak identity provider |
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

- **Checkstyle** - Code style validation
- **PMD** - Static code analysis
- **SpotBugs** - Bug pattern detection

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
