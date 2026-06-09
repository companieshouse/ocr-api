# API Endpoints — ocr-api

> All paths are prefixed by the value of the `api.endpoint` property, which is set to `/ocr-api` in `application.properties`. The full base URL in production is routed by the ALB listener rule matching `/ocr-api/*`.

---

## Exposed Endpoints

---

### `POST /ocr-api/api/ocr/image/tiff/extractText`
**Synchronous OCR — direct file upload**

The caller uploads a TIFF image as a multipart form field. The service processes the image on the OCR thread pool, blocks until processing is complete, and returns the full result in the HTTP response body. The caller must hold the connection open for the duration of processing.

**Controller:** `SyncImageOcrApiController.extractTextFromTiff()`
**File:** `src/main/java/.../image/extracttext/SyncImageOcrApiController.java:52`

#### Request

| Type | Parameter | Required | Description |
| --- | --- | --- | --- |
| Multipart form | `file` | ✅ | The TIFF image to process |
| Query / form | `responseId` | ✅ | Caller-supplied identifier echoed back in the result |
| Query / form | `contextId` | ❌ | Correlation identifier for tracing; defaults to `responseId` if absent or blank |

**Content-Type:** `multipart/form-data`
**Max file size:** 100 MB (`spring.servlet.multipart.max-file-size`)

#### Response

**`200 OK`** — OCR completed successfully.

```json
{
  "context_id": "my-app-12345",
  "response_id": "12345",
  "extracted_text": "ARTICLES OF ASSOCIATION\n...",
  "average_confidence_score": 87,
  "lowest_confidence_score": 42,
  "ocr_processing_time_ms": 3210,
  "total_processing_time_ms": 3450,
  "result_code": 0
}
```

**`503 Service Unavailable`** — OCR thread pool queue is full.

```json
{
  "context_id": "my-app-12345",
  "error_message": "Service Overloaded"
}
```

**`500 Internal Server Error`** — OCR conversion or unexpected failure.

```json
{
  "error_message": "Unexpected Error Before OCR Conversion"
}
```

---

### `POST /ocr-api/api/ocr/image/tiff/extractTextRequest`
**Asynchronous OCR — callback pattern**

The caller submits a JSON body containing URLs for image retrieval and result delivery. The service validates the request and both URLs, enqueues the work, and immediately returns `HTTP 202 Accepted`. Processing happens in the background; when complete the service POSTs the result to `converted_text_endpoint`. On failure the service also POSTs an error payload to `converted_text_endpoint`.

**Controller:** `AsyncImageOcrApiController.receiveOcrRequest()`
**File:** `src/main/java/.../image/extracttext/AsyncImageOcrApiController.java:61`

#### Request

**Content-Type:** `application/json`

```json
{
  "app_id": "my-calling-service",
  "response_id": "12345",
  "image_endpoint": "https://internal-service.example.com/documents/12345/image",
  "converted_text_endpoint": "https://internal-service.example.com/documents/12345/ocr-result"
}
```

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| `app_id` | `String` | ✅ | `@NotBlank` | Identifier of the calling application |
| `response_id` | `String` | ✅ | `@NotBlank` | Caller-supplied identifier echoed back in the result |
| `image_endpoint` | `String` | ✅ | `@NotBlank` + hostname whitelist | URL the service will `GET` to retrieve the TIFF image |
| `converted_text_endpoint` | `String` | ✅ | `@NotBlank` + hostname whitelist | URL the service will `POST` the OCR result back to |

#### Response

**`202 Accepted`** — Request enqueued for asynchronous processing. No response body.

**`400 Bad Request`** — Validation failure (missing/blank fields or malformed JSON).

```json
{
  "context_id": "UNKNOWN",
  "response_id": "UNKNOWN",
  "error_message": "invalid input[Missing required value app_id]"
}
```

**`400 Bad Request`** — A supplied URL failed hostname whitelist validation.

```json
{
  "context_id": "my-calling-service-12345",
  "error_message": "Bad Url contained in OCR Request"
}
```

**`503 Service Unavailable`** — OCR thread pool queue is full.

```json
{
  "context_id": "my-calling-service-12345",
  "error_message": "Service Overloaded"
}
```

**`500 Internal Server Error`** — Unexpected controller-level failure.

```json
{
  "error_message": "Unexpected Error Before OCR Conversion"
}
```

> **Async callback payload** — When processing completes (or fails), the service `POST`s the following to `converted_text_endpoint`. The shape is the same as the sync `200 OK` response above. On error, `extracted_text` is `"UNABLE_TO_PROCESS_OCR_CONVERSION"` and `result_code` is non-zero.

---

### `GET /ocr-api/healthcheck`
**Liveness check**

Returns a plain-text confirmation that the service is running. Logs current thread pool statistics on every call. Used by the ECS task container health check and the ALB target group health check (`healthcheck_matcher = "200"` in `locals.tf`).

**Controller:** `HealthCheckController.heathCheck()`
**File:** `src/main/java/.../healthcheck/HealthCheckController.java:27`

#### Request
No parameters.

#### Response

**`200 OK`**

```
ocr-api is alive
```

---

### `GET /ocr-api/statistics`
**Internal thread pool metrics**

Returns live state of the OCR thread pool executor. Intended for internal monitoring and operational dashboards.

**Controller:** `StatisticsController.getStatistics()`
**File:** `src/main/java/.../statistics/StatisticsController.java:20`

#### Request
No parameters.

#### Response

**`200 OK`**

```json
{
  "instance_uuid": "a3f2c1d4-...",
  "queue_size": 2,
  "pool_size": 4,
  "active_pool_size": 3,
  "largest_pool_size": 4
}
```

| Field | Description |
| --- | --- |
| `instance_uuid` | Random UUID generated once at startup; identifies this specific running instance |
| `queue_size` | Requests currently waiting in the executor queue |
| `pool_size` | Current number of threads in the pool |
| `active_pool_size` | Threads currently executing an OCR task |
| `largest_pool_size` | Peak thread count since the service started |

---

# Consumed / External APIs

The `ocr-api` makes two categories of outbound HTTP call in the **asynchronous flow**. Both use the shared `RestTemplate` bean configured with a `rest.client.timeout.seconds` connect/read timeout (default `300` seconds). All target hostnames must be present on the `HOST_WHITE_LIST` before the call is made.

---

### Image Fetch — `GET {image_endpoint}`

**Client:** `ImageRestClient.getImageContentsFromEndpoint()`
**File:** `src/main/java/.../image/extracttext/ImageRestClient.java:26`

| Detail | Value |
| --- | --- |
| Method | `GET` |
| URL | Caller-supplied `image_endpoint` value |
| Response type | `byte[]` (raw TIFF image bytes) |
| On failure | Throws `OcrRequestException(FAIL_TO_GET_IMAGE)`; triggers error callback to `converted_text_endpoint` |

The URL is validated against the hostname whitelist before this call is made. The full URL (including any query string) is passed directly to `RestTemplate.getForEntity()`. No authentication headers are added by this service.

---

### Result Callback — `POST {converted_text_endpoint}`

**Client:** `CallbackExtractedTextRestClient.sendTextResult()` / `sendTextResultError()`
**File:** `src/main/java/.../image/extracttext/CallbackExtractedTextRestClient.java:31`

| Detail | Value |
| --- | --- |
| Method | `POST` |
| URL | Caller-supplied `converted_text_endpoint` value |
| Request body | `ExtractTextResultDto` serialised as JSON |
| Content-Type | `application/json` (set by `RestTemplate` Jackson serialisation) |
| On success callback failure | Throws `OcrRequestException(FAIL_TO_SEND_RESULTS)` |
| On error callback failure | Exception is swallowed and logged; no further recovery |

The `sendTextResultError()` variant is always called on failure paths to ensure the caller is notified even if OCR processing itself could not complete.

---

# Swagger / OpenAPI Specification

The `ocr-api` **does not expose a Swagger UI or OpenAPI specification**. There is no `springdoc-openapi`, `springfox`, or equivalent dependency in `pom.xml`, and no OpenAPI configuration class in the codebase.

The API contract is defined implicitly by:
- The `OcrClientRequest` DTO and its `@NotBlank` constraints (async endpoint)
- The `@RequestParam` declarations in `SyncImageOcrApiController` (sync endpoint)
- The `ExtractTextResultDto` and `ErrorResponseDto` response shapes

If OpenAPI documentation is required, adding `springdoc-openapi-starter-webmvc-ui` to `pom.xml` and annotating the controllers with `@Operation` / `@ApiResponse` would generate a `/swagger-ui.html` endpoint automatically.

---

# Authentication & Authorisation

The `ocr-api` currently has **no authentication or authorisation mechanism**.

- There is no `spring-boot-starter-security` dependency.
- There are no API keys, JWT tokens, OAuth2 scopes, or mTLS requirements on any endpoint.
- The Terraform configuration explicitly disables the Companies House ERIC reverse proxy (`use_eric_reverse_proxy = false` in `terraform/groups/ecs-service/main.tf:86`), which is the standard CH API gateway/auth layer.

Access control is currently **entirely network-level**, relying on VPC placement and ALB listener rules that restrict the path `/ocr-api/*` to traffic within the internal network. Any service that can reach the ALB on the correct path can call all four endpoints without credentials.

The only input-validation security control present is the **hostname whitelist** (`UrlHostValidator`) on the async endpoint, which prevents the service from being used to make outbound HTTP calls to arbitrary hosts. This mitigates SSRF but provides no caller identity verification.

> ⚠️ See finding #1 in `security-review.md` for the full risk assessment and remediation recommendations.

---

# Summary Table

| Method | Path | Purpose | Request Format | Response Format | Auth Required |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/ocr-api/api/ocr/image/tiff/extractText` | Synchronous OCR — upload TIFF, receive extracted text in response | `multipart/form-data` (`file`, `responseId`, `contextId?`) | `application/json` — `ExtractTextResultDto` | ❌ None |
| `POST` | `/ocr-api/api/ocr/image/tiff/extractTextRequest` | Asynchronous OCR — submit URLs, receive result via callback POST | `application/json` — `OcrClientRequest` | `202 Accepted` (no body); result delivered asynchronously to `converted_text_endpoint` | ❌ None |
| `GET` | `/ocr-api/healthcheck` | Liveness check for ECS and ALB health probes | None | `text/plain` — `"ocr-api is alive"` | ❌ None |
| `GET` | `/ocr-api/statistics` | Internal thread pool metrics | None | `application/json` — `StatisticsDto` | ❌ None |
| `GET` | `{image_endpoint}` *(consumed)* | Fetch the TIFF image from the caller's system | None (GET) | `byte[]` raw image | Caller-managed |
| `POST` | `{converted_text_endpoint}` *(consumed)* | Deliver OCR result or error payload back to the caller's system | `application/json` — `ExtractTextResultDto` | Caller-defined | Caller-managed |
