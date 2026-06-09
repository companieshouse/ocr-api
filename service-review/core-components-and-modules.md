# Core Components and Modules — ocr-api

## Major Modules, Directories and Their Purpose

| Module/Directory | Purpose | Key Classes/Functions |
| --- | --- | --- |
| `src/main/java/.../ocr/api` | Root application package. Bootstraps the Spring Boot application and holds cross-cutting configuration beans. | `OcrApiApplication`, `SpringConfiguration`, `RestTemplateConfig`, `ThreadConfig` |
| `src/main/java/.../ocr/api/image/extracttext` | Core feature package. Contains all logic for receiving OCR requests, processing images, managing the async/sync workflows, and sending results back to callers. | `AsyncImageOcrApiController`, `SyncImageOcrApiController`, `OcrRequestService`, `ImageOcrService`, `ImageRestClient`, `CallbackExtractedTextRestClient` |
| `src/main/java/.../ocr/api/urlvalidator` | URL validation and SSRF protection. Validates that caller-supplied URLs are well-formed and that their hostnames appear on a configured allowlist before any outbound HTTP call is made. | `UrlHostValidator`, `UrlValidatorException` |
| `src/main/java/.../ocr/api/healthcheck` | Liveness endpoint. Returns `200 OK` with a plain-text message and logs current thread pool statistics on every call. | `HealthCheckController` |
| `src/main/java/.../ocr/api/statistics` | Internal metrics endpoint. Exposes live thread pool state (queue depth, pool size, active threads, instance UUID) as a JSON response. | `StatisticsController`, `StatisticsService`, `StatisticsDto` |
| `src/main/java/.../ocr/api/logging` | HTTP request/response logging. Registers a `HandlerInterceptor` that writes structured log entries at the start and end of every inbound HTTP request. | `LoggingInterceptor`, `InterceptorConfig` |
| `src/main/java/.../ocr/api/common` | Shared constants, enums, and DTOs used across the application. | `JsonConstants`, `CallTypeEnum`, `ErrorResponseDto`, `OcrGeneralConstants`, `LogFieldValue` |
| `src/main/java/.../ocr/api/tesseract` | Tesseract-specific constants (e.g. language code). Isolates engine configuration from business logic. | `TesseractConstants` |
| `src/main/resources` | Spring Boot application configuration. Defines multipart upload limits, queue capacity, confidence threshold, whitelist, tessdata path, and REST client timeout — all resolved from environment variables at runtime. | `application.properties` |
| `src/test/java/.../ocr/api` | Unit and integration tests. Covers controllers, services, URL validation, and REST client behaviour using JUnit 5 and Spring MockMvc. | `IntegrationTest`, `HealthCheckControllerTest`, `StatisticsControllerTest`, `StatisticsServiceTest`, `UrlHostValidatorTest`, `RestTemplateConfigTest`, `TestObjectMother` |
| `src/test/resources` | Test fixtures. Real TIFF files and expected text output used by integration tests. | `sample-articles-of-association.tif`, `sample-articles-of-association.txt`, `blank-articles.tif`, `empty-articles.tif`, `small-articles.tif` |
| `docker-resources/` | Runtime assets for the development Docker image. Contains the Tesseract English language model (`eng.traineddata`) and a Maven settings configuration script. | `tessdata/2020-10-21/eng.traineddata`, `configure-maven` |
| `ecs-image-build/` | Production container definition. Builds the ECS-deployable image on top of the Companies House Corretto 21 base image, installing Tesseract from the AL2023 package repository. | `Dockerfile`, `docker_start.sh` |
| `Dockerfile` | Development / local container definition. Builds on `eclipse-temurin:21-jre`, installs Tesseract via `apt-get`, and copies in the local JAR. | — |
| `terraform/groups/ecs-service/` | Infrastructure-as-code for AWS deployment. Provisions the ECS service, ALB listener rules, SSM Parameter Store secrets, and Vault-sourced configuration. | `main.tf`, `locals.tf`, `variables.tf`, `data.tf`, `vault.tf` |

---

## Key Classes, Components or Functions

### `OcrApiApplication`
Entry point. Annotated with `@SpringBootApplication`; starts the embedded Tomcat container. Defines the application namespace constant (`"ocr-api"`) used as the structured logging prefix throughout the codebase.

### `SpringConfiguration`
`@Configuration` bean that reads all environment-driven properties via `@Value` injection and exposes them as typed getters. Logs all resolved property values at startup via `@PostConstruct` for operational visibility.

### `ThreadConfig`
Declares the `ThreadPoolTaskExecutor` bean (`ocrRequestTaskExecutor`) that backs all OCR processing. Pool size is read from the `OCR_TESSERACT_THREAD_POOL_SIZE` environment variable (default: 4). The `@EnableAsync` annotation on this class activates Spring's asynchronous method execution infrastructure.

### `RestTemplateConfig`
Produces the shared `RestTemplate` bean used for all outbound HTTP calls (image fetch and callback). Applies configurable connect and read timeouts sourced from `SpringConfiguration`.

### `AsyncImageOcrApiController`
Handles `POST /ocr-api/api/ocr/image/tiff/extractTextRequest`. Validates the inbound JSON body (`@Valid @RequestBody OcrClientRequest`), validates both caller-supplied URLs against the hostname whitelist, submits the request to the async thread pool, and immediately returns `HTTP 202 Accepted`. Owns exception handlers for `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, and a catch-all `Exception` handler.

### `SyncImageOcrApiController`
Handles `POST /ocr-api/api/ocr/image/tiff/extractText`. Accepts a multipart TIFF file upload, submits the bytes to the OCR thread pool via `OcrRequestService.handleSynchronousRequest()`, blocks on the `CompletableFuture` result, and returns the full `ExtractTextResultDto` in the response body.

### `AbstractOcrApiController`
Shared base class for both controllers. Provides the `OcrRequestException` handler (maps result codes to HTTP status codes) and the `logClientRequest()` helper that writes structured request log entries including client IP and referer header.

### `OcrRequestService`
Service that orchestrates the full OCR workflow for both call types.
- `handleAsynchronousRequest()` — `@Async` method: fetches the image via `ImageRestClient`, calls `ImageOcrService`, transforms the result, and POSTs it back via `CallbackExtractedTextRestClient`. Error cases also trigger a callback with an error payload.
- `handleSynchronousRequest()` — `@Async` method returning `CompletableFuture<TextConversionResult>`: delegates directly to `ImageOcrService` with the already-supplied image bytes.

### `ImageOcrService`
Wraps the Tesseract C API via `tess4j`. Core method `extractTextFromImageBytes()`:
1. Reads the TIFF byte array into an `ImageInputStream`.
2. Opens a Tesseract `TessBaseAPI` handle, initialised with the configured `tessdata` path and English language model.
3. Iterates over every page in the TIFF, feeds each rendered `BufferedImage` to Tesseract, and collects UTF-8 text and per-line confidence scores.
4. Lines below the configured `lowConfidenceToLog` threshold are written to debug logs.
5. Returns a `TextConversionResult` containing the full document text and aggregated confidence statistics.

### `ImageRestClient`
Thin wrapper around `RestTemplate.getForEntity()` that fetches a TIFF image as a `byte[]` from a caller-supplied URL. Wraps all exceptions in `OcrRequestException(FAIL_TO_GET_IMAGE)`.

### `CallbackExtractedTextRestClient`
Posts `ExtractTextResultDto` (success or error) back to the caller's `converted_text_endpoint` via `RestTemplate.postForEntity()`. The error variant (`sendTextResultError`) never re-throws — it logs and swallows exceptions because there is no further recovery path once a callback fails.

### `UrlHostValidator`
Guards against SSRF on the async endpoint. Validates a URL string in two stages: (1) structural validity via Apache Commons `UrlValidator`; (2) hostname membership in a comma-separated allowlist read from the `HOST_WHITE_LIST` environment variable. Throws `UrlValidatorException` on any failure.

### `OcrClientRequest`
Inbound JSON request DTO for the async endpoint. Uses `@NotBlank` Bean Validation constraints on all four fields (`app_id`, `response_id`, `image_endpoint`, `converted_text_endpoint`).

### `OcrRequest`
Internal domain object created from `OcrClientRequest`. Trims all string fields and derives `contextId` as `applicationId + '-' + responseId` for use as a structured logging correlation key.

### `ExtractTextResultDto`
Outbound JSON response DTO. Carries `extracted_text`, `average_confidence_score`, `lowest_confidence_score`, `ocr_processing_time_ms`, `total_processing_time_ms`, `response_id`, `context_id`, and `result_code`. The static factory method `createErrorExtractTextResultDtoFromContextId()` builds a standardised error payload.

### `TextConversionResult`
Domain model that accumulates OCR output during processing. Maintains a `StopWatch` for timing, a document-level and per-page `Confidence` aggregator, and the final extracted text string set by `completeSuccess()`.

### `Confidence`
Value object that tracks the running sum, count, and minimum of Tesseract confidence values across a set of text lines. Computes average and minimum on demand. Used at both page level and document level within `TextConversionResult`.

### `OcrRequestException`
Checked exception carrying a `ResultCode` enum that pairs a numeric code, an `HttpStatus`, and a human-readable message. Result codes cover: `FAIL_TO_GET_IMAGE`, `FAIL_TO_COVERT_IMAGE_TO_TEXT`, `UNEXPECTED_FAILURE`, `BAD_REQUEST`, `BAD_URL`, `APPLICATION_OVERLOADED`, and `FAIL_TO_SEND_RESULTS`.

### `MonitoringService` / `MonitoringFields`
Structured logging facade for per-request monitoring events. `MonitoringFields` assembles a log map containing confidence scores, processing times, file size, page count, result code, and call type. Consumed by `OcrRequestService` and the controllers to emit a single monitoring log record per transaction.

### `StatisticsService`
Reads live metrics from the `ThreadPoolTaskExecutor` (queue depth, pool size, active count, largest pool size) and packages them with a static random `INSTANCE_UUID` into a `StatisticsDto` for the statistics endpoint.

### `LoggingInterceptor` / `InterceptorConfig`
`HandlerInterceptor` implementation that delegates to the Companies House `RequestLogger` interface. `preHandle` logs the start of each request; `postHandle` logs the end with the response status. Registered globally via `InterceptorConfig`.

---

## Structure Pattern

The application follows a **layered, package-by-feature** structure within a standard Spring Boot project layout.

```
ocr-api/
├── src/
│   ├── main/
│   │   ├── java/.../ocr/api/
│   │   │   ├── (root)                  ← Bootstrap + cross-cutting config
│   │   │   ├── image/extracttext/      ← Feature package: controllers, services, DTOs, clients
│   │   │   ├── urlvalidator/           ← SSRF guard
│   │   │   ├── healthcheck/            ← Liveness endpoint
│   │   │   ├── statistics/             ← Internal metrics endpoint
│   │   │   ├── logging/                ← HTTP request/response interceptor
│   │   │   ├── common/                 ← Shared constants, enums, base DTOs
│   │   │   └── tesseract/              ← OCR engine constants
│   │   └── resources/
│   │       └── application.properties  ← Environment-variable-backed config
│   └── test/
│       ├── java/.../ocr/api/           ← Unit + integration tests (mirrors main structure)
│       └── resources/                  ← TIFF test fixtures
├── docker-resources/                   ← Dev Docker assets (tessdata, Maven config)
├── ecs-image-build/                    ← Production ECS Dockerfile + start script
├── terraform/groups/ecs-service/       ← AWS infrastructure (ECS, ALB, SSM, Vault)
├── Dockerfile                          ← Local development image
├── Tiltfile.dev                        ← Live-reload dev environment (Tilt + Jib)
├── Makefile                            ← Developer task runner
└── pom.xml                             ← Maven build, dependency management, CVE overrides
```

**Architectural style:** The service follows a classic **Controller → Service → Client** layering pattern:

- **Controllers** (`AsyncImageOcrApiController`, `SyncImageOcrApiController`) handle HTTP concerns: request validation, URL validation, error mapping, and response shaping.
- **Services** (`OcrRequestService`, `ImageOcrService`) own the business logic: workflow orchestration, image processing, confidence scoring.
- **Clients** (`ImageRestClient`, `CallbackExtractedTextRestClient`) isolate all outbound HTTP communication behind a single `RestTemplate`.
- **Configuration classes** (`SpringConfiguration`, `ThreadConfig`, `RestTemplateConfig`) wire together the runtime environment and are kept strictly separate from feature logic.
- **Cross-cutting concerns** (logging, monitoring) are handled via an interceptor and a dedicated `MonitoringService`, keeping feature classes free of observability boilerplate.

The async and sync call paths share the same `ImageOcrService` (OCR processing) and `MonitoringService` (telemetry) — the only divergence is in how the image bytes arrive (fetched remotely vs uploaded directly) and how the result is delivered (callback POST vs synchronous HTTP response).
