# Data Models — ocr-api

> **Note:** The `ocr-api` has no persistent data store (no database, no ORM, no message broker). All models are in-memory Java objects that exist for the lifetime of a single request. The sections below document the request/response DTOs, internal domain objects, and value objects that form the application's data layer.

---

# Core Data Models

---

## `OcrClientRequest`
*Package:* `uk.gov.companieshouse.ocr.api.image.extracttext`
*File:* `OcrClientRequest.java`

The inbound JSON request body for the **asynchronous** OCR endpoint (`POST /ocr-api/api/ocr/image/tiff/extractTextRequest`). This is the external contract surface — the shape of JSON a calling service must send.

All four fields carry `@NotBlank` Bean Validation constraints; a missing or blank value causes Spring to return `HTTP 400` before the controller body is reached.

| Field (JSON) | Java Field | Type | Validation | Description |
| --- | --- | --- | --- | --- |
| `app_id` | `applicationId` | `String` | `@NotBlank` | Identifier of the calling application |
| `response_id` | `responseId` | `String` | `@NotBlank` | Caller-supplied identifier echoed back in the result |
| `image_endpoint` | `imageEndpoint` | `String` | `@NotBlank` | URL the service will `GET` to fetch the TIFF image |
| `converted_text_endpoint` | `convertedTextEndpoint` | `String` | `@NotBlank` | URL the service will `POST` the OCR result back to |

---

## `OcrRequest`
*Package:* `uk.gov.companieshouse.ocr.api.image.extracttext`
*File:* `OcrRequest.java`

Internal domain object created immediately from an `OcrClientRequest` at the controller boundary. All string fields are trimmed on construction. This object is passed through the async processing pipeline and is never serialised to JSON.

| Field | Type | Source / Derivation | Description |
| --- | --- | --- | --- |
| `applicationId` | `String` | `StringUtils.trim(clientRequest.getApplicationId())` | Trimmed application identifier |
| `responseId` | `String` | `StringUtils.trim(clientRequest.getResponseId())` | Trimmed response identifier |
| `imageEndpoint` | `String` | `StringUtils.trim(clientRequest.getImageEndpoint())` | Validated + trimmed image fetch URL |
| `convertedTextEndpoint` | `String` | `StringUtils.trim(clientRequest.getConvertedTextEndpoint())` | Validated + trimmed callback URL |
| `contextId` | `String` | `applicationId + '-' + responseId` | Derived correlation key used in all structured log entries |
| `timeRequestReceived` | `LocalDateTime` | `LocalDateTime.now()` at controller entry | Timestamp when the HTTP request was received |

---

## `TextConversionResult`
*Package:* `uk.gov.companieshouse.ocr.api.image.extracttext`
*File:* `TextConversionResult.java`

The central domain object built up during OCR processing inside `ImageOcrService`. It is mutable and accumulates data as pages are processed. It is never serialised directly — `ImageOcrTransformer` maps it to `ExtractTextResultDto` before it leaves the service layer.

| Field | Type | Description |
| --- | --- | --- |
| `contextId` | `String` | Correlation identifier, set on construction |
| `responseId` | `String` | Response identifier, set on construction |
| `fileSize` | `int` | Byte length of the input image, set on construction |
| `timeOnExecuterQueue` | `long` | Milliseconds the request waited on the thread pool queue before processing began |
| `extractedText` | `String` | Full concatenated UTF-8 text of all pages; set by `completeSuccess()` when processing finishes |
| `documentConfidence` | `Confidence` | Running aggregate of confidence scores across the entire document |
| `currentPageConfidence` | `Confidence` | Running aggregate for the page currently being processed; replaced by `addPage()` |
| `pageConfidences` | `List<Confidence>` | One `Confidence` entry per page, in order |
| `extractTextWatch` | `StopWatch` | Started on construction, stopped by `completeSuccess()`; measures pure OCR processing time |

**Key methods:**

| Method | Behaviour |
| --- | --- |
| `addPage()` | Creates a new `Confidence` object for the next page and appends it to `pageConfidences` |
| `addConfidence(float)` | Adds a line-level confidence value to both `documentConfidence` and `currentPageConfidence` |
| `completeSuccess(String)` | Sets `extractedText` and stops the stopwatch |
| `createForZeroLengthFile(...)` | Static factory returning an instance with `fileSize = 0` and no text; used for empty input |
| `metaDataMap()` | Returns a `Map<String, Object>` of all metadata fields for structured logging |

---

## `Confidence`
*Package:* `uk.gov.companieshouse.ocr.api.image.extracttext`
*File:* `Confidence.java`

Immutable-by-convention value object used as both a page-level and document-level confidence aggregator within `TextConversionResult`. Tesseract returns a `float` confidence value (0–100) for each text line; this object accumulates those values.

| Field | Type | Description |
| --- | --- | --- |
| `minimum` | `Float` | Lowest confidence value seen so far; `null` until the first value is added |
| `numberOfDataPoints` | `int` | Count of confidence values added |
| `sum` | `Float` | Running sum of all confidence values |

**Key methods:**

| Method | Behaviour |
| --- | --- |
| `addConfidenceValue(float)` | Increments count, adds to sum, updates minimum if the new value is lower |
| `getAverage()` | Returns `sum / numberOfDataPoints`; returns `0.0` if no data points exist |
| `getMinimum()` | Returns `minimum`; returns `0.0` if no data points exist |

---

## `ExtractTextResultDto`
*Package:* `uk.gov.companieshouse.ocr.api.image.extracttext`
*File:* `ExtractTextResultDto.java`

The outbound JSON response DTO returned to callers — either directly in the HTTP response body (sync endpoint) or POSTed to the `converted_text_endpoint` (async endpoint). Produced by `ImageOcrTransformer.mapModelToApi(TextConversionResult)`.

| Field (JSON) | Java Field | Type | Description |
| --- | --- | --- | --- |
| `context_id` | `contextId` | `String` | Echo of the request context identifier |
| `response_id` | `responseId` | `String` | Echo of the caller-supplied response identifier |
| `extracted_text` | `extractedText` | `String` | Full OCR text extracted from the document; `"UNABLE_TO_PROCESS_OCR_CONVERSION"` on error |
| `average_confidence_score` | `averageConfidenceScore` | `int` | Rounded average Tesseract line confidence (0–100) across the whole document |
| `lowest_confidence_score` | `lowestConfidenceScore` | `int` | Rounded minimum Tesseract line confidence (0–100) across the whole document |
| `ocr_processing_time_ms` | `ocrProcessingTimeMs` | `long` | Time spent in the Tesseract OCR engine (milliseconds) |
| `total_processing_time_ms` | `totalProcessingTimeMs` | `long` | Total end-to-end processing time including queue wait (milliseconds) |
| `result_code` | `resultCode` | `int` | `0` = success; positive integer = specific error code (see `OcrRequestException.ResultCode`) |

**Static factory:**
`createErrorExtractTextResultDtoFromContextId(contextId, responseId, errorResultCode, totalProcessingTimeMs)` — builds a standard error payload with zeroed scores and `extracted_text` set to `"UNABLE_TO_PROCESS_OCR_CONVERSION"`.

---

## `ErrorResponseDto`
*Package:* `uk.gov.companieshouse.ocr.api.common`
*File:* `ErrorResponseDto.java`

Returned as the HTTP response body whenever a request fails at the controller layer (validation failure, bad URL, capacity exceeded, unexpected exception).

| Field (JSON) | Java Field | Type | Description |
| --- | --- | --- | --- |
| `context_id` | `contextId` | `String` | Context identifier; may be `"UNKNOWN"` if the failure occurred before the request was parsed |
| `response_id` | `responseId` | `String` | Response identifier; may be `"UNKNOWN"` if not yet available |
| `error_message` | `errorMessage` | `String` | Human-readable error description |

---

## `StatisticsDto`
*Package:* `uk.gov.companieshouse.ocr.api.statistics`
*File:* `StatisticsDto.java`

Returned by `GET /ocr-api/statistics`. Reflects the live state of the OCR thread pool executor.

| Field (JSON) | Java Field | Type | Description |
| --- | --- | --- | --- |
| `instance_uuid` | `instanceUuid` | `String` | Random UUID generated once at application startup; identifies the running instance |
| `queue_size` | `queueSize` | `int` | Number of requests currently waiting in the executor queue |
| `pool_size` | `poolSize` | `int` | Current number of threads in the pool |
| `active_pool_size` | `activePoolSize` | `int` | Number of threads actively executing an OCR task |
| `largest_pool_size` | `largestPoolSize` | `int` | Historical peak thread count since startup |

---

## `MonitoringFields`
*Package:* `uk.gov.companieshouse.ocr.api.image.extracttext`
*File:* `MonitoringFields.java`

An internal, non-serialised DTO used exclusively for structured logging. One `MonitoringFields` instance is emitted per completed OCR transaction (success or failure) by `MonitoringService`. It is never returned in an HTTP response.

| Field | Type | Description |
| --- | --- | --- |
| `averageConfidenceScore` | `int` | Document average confidence |
| `lowestConfidenceScore` | `int` | Document minimum confidence |
| `ocrProcessingTimeMs` | `long` | Pure Tesseract processing time |
| `totalProcessingTimeMs` | `long` | Full end-to-end time |
| `timeOnExecuterQueue` | `long` | Time spent waiting in the thread pool queue |
| `totalPages` | `Integer` | Number of pages in the processed document |
| `fileSize` | `int` | Input image size in bytes |
| `callType` | `CallTypeEnum` | `SYNCHRONOUS` or `ASYNCHRONOUS` |
| `resultCode` | `int` | Numeric result code (`0` = success) |

---

## `OcrRequestException.ResultCode` (Enum)
*Package:* `uk.gov.companieshouse.ocr.api.image.extracttext`
*File:* `OcrRequestException.java` (inner enum)

Defines all result/error codes used in `ExtractTextResultDto.result_code` and in `ErrorResponseDto` HTTP status mapping.

| Enum Value | Code | HTTP Status | Description |
| --- | --- | --- | --- |
| `FAIL_TO_GET_IMAGE` | 1 | 500 | Could not fetch the TIFF from `image_endpoint` |
| `FAIL_TO_COVERT_IMAGE_TO_TEXT` | 2 | 500 | Tesseract processing failed |
| `UNEXPECTED_FAILURE` | 3 | 500 | Uncategorised internal error |
| `BAD_REQUEST` | 400 | 400 | Malformed or missing request fields |
| `BAD_URL` | 400 | 400 | A supplied URL failed hostname whitelist validation |
| `INTERNAL_SERVER_ERROR` | 500 | 500 | Generic internal error returned from the exception handler |
| `APPLICATION_OVERLOADED` | 503 | 503 | Thread pool queue is full; request rejected |
| `FAIL_LOGIC_ERROR` | 998 | 500 | Application logic error (logging only) |
| `FAIL_TO_SEND_RESULTS` | 999 | 500 | Could not POST result to `converted_text_endpoint` (logging only) |

---

# Model Relationships

```
OcrClientRequest  ──(constructed into)──►  OcrRequest
                                               │
                              ┌────────────────┤
                              │                │
                      imageEndpoint    convertedTextEndpoint
                              │                │
                    ImageRestClient    CallbackExtractedTextRestClient
                              │                │
                              ▼                ▼
                         byte[]          ExtractTextResultDto
                              │
                    ImageOcrService
                              │
                    TextConversionResult
                         │         │
                  documentConfidence  List<Confidence>  (one per page)
                         │
                 ImageOcrTransformer
                              │
                              ▼
                    ExtractTextResultDto  ──(on error)──►  ErrorResponseDto
                              │
                         MonitoringFields
                              │
                         MonitoringService  ──► structured log
```

**Key relationships:**

| Relationship | Description |
| --- | --- |
| `OcrClientRequest` → `OcrRequest` | 1-to-1; `OcrRequest` is created by trimming and enriching `OcrClientRequest` at the controller boundary. `OcrClientRequest` is discarded after this point. |
| `OcrRequest` → `TextConversionResult` | 1-to-1 per transaction; `contextId` and `responseId` are propagated from `OcrRequest` into `TextConversionResult` for tracing. |
| `TextConversionResult` → `Confidence` | 1-to-many; one `Confidence` object per page plus one document-level aggregate. All are owned by and updated through `TextConversionResult`. |
| `TextConversionResult` → `ExtractTextResultDto` | 1-to-1; `ImageOcrTransformer.mapModelToApi()` projects the domain object into the API DTO. The domain object is not exposed outside the service layer. |
| `ExtractTextResultDto` → `MonitoringFields` | 1-to-1 per transaction; `MonitoringFields` is composed from both `TextConversionResult` and `ExtractTextResultDto` to produce the single monitoring log event. |
| `OcrRequestException.ResultCode` → `ExtractTextResultDto.result_code` | The numeric `code` value of the enum is written into the DTO's `resultCode` field on error paths. |
| `OcrRequestException.ResultCode` → `ErrorResponseDto` | The enum's `httpStatus` drives the HTTP response code; its `message` drives the `error_message` field. |

---

# Migrations

This service has **no database and no migration history**. There is no ORM (no JPA/Hibernate, no Spring Data), no schema, and no migration tooling (no Flyway, no Liquibase).

All data models are purely in-memory Java objects scoped to the lifetime of a single HTTP request. State is not persisted between requests.

If a persistent store is introduced in future, a migration framework such as **Flyway** should be adopted at that point.

---

# Summary Table

| Model/Class | Purpose | Key Fields/Relationships |
| --- | --- | --- |
| `OcrClientRequest` | Inbound JSON request DTO for the async endpoint; defines the external API contract | `app_id`, `response_id`, `image_endpoint`, `converted_text_endpoint` — all `@NotBlank`; mapped to `OcrRequest` |
| `OcrRequest` | Internal domain object for the async processing pipeline; derived from `OcrClientRequest` | `contextId` (derived), `imageEndpoint`, `convertedTextEndpoint`, `timeRequestReceived`; carries `TextConversionResult` state indirectly via `contextId` |
| `TextConversionResult` | Mutable accumulator built up during OCR processing; the central domain model | `extractedText`, `documentConfidence`, `pageConfidences (List<Confidence>)`, `extractTextWatch`; mapped to `ExtractTextResultDto` by `ImageOcrTransformer` |
| `Confidence` | Value object that aggregates Tesseract confidence scores for a set of text lines | `minimum`, `sum`, `numberOfDataPoints`; composed 1+ times inside `TextConversionResult` |
| `ExtractTextResultDto` | Outbound JSON response DTO for both sync response and async callback | `extracted_text`, `average_confidence_score`, `lowest_confidence_score`, `result_code`, timing fields; produced from `TextConversionResult` |
| `ErrorResponseDto` | Outbound JSON error response for controller-level failures | `context_id`, `response_id`, `error_message`; HTTP status driven by `OcrRequestException.ResultCode` |
| `StatisticsDto` | JSON response for the internal statistics endpoint | `instance_uuid`, `queue_size`, `pool_size`, `active_pool_size`, `largest_pool_size`; populated from `ThreadPoolTaskExecutor` |
| `MonitoringFields` | Internal logging DTO; not serialised to HTTP responses | Composed from `TextConversionResult` + `ExtractTextResultDto`; emitted once per transaction via `MonitoringService` |
| `OcrRequestException.ResultCode` | Enum mapping error conditions to numeric codes and HTTP statuses | `code (int)`, `httpStatus (HttpStatus)`, `message (String)`; drives both `result_code` in DTOs and HTTP response status codes |
