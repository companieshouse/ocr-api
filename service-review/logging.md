# Logging

The application uses the Companies House **`structured-logging`** library (`uk.gov.companieshouse:structured-logging:3.0.57`) throughout. All log output is written as structured JSON, making it directly queryable in CloudWatch Logs Insights in production. Every logger is instantiated with the application namespace `"ocr-api"`:

```java
// Used in every class that logs
private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
// APPLICATION_NAME_SPACE = "ocr-api"
```

### Log Levels in Use

| Level | Where used |
| --- | --- |
| `INFO` | Startup config dump, request received, post-queue processing, OCR completion metadata, monitoring events, health check stats |
| `DEBUG` | URL validation, image fetch URL, page count, low-confidence line detail |
| `TRACE` | Per-line confidence values during OCR iteration |
| `ERROR` | Caught exceptions — passed to `LOG.errorContext()` or `LOG.error()` with the full exception |

### HTTP Request / Response Logging

Every inbound HTTP request is logged by `LoggingInterceptor`, which implements the CH `RequestLogger` interface and is registered globally via `InterceptorConfig`:

- **`preHandle`** — logs the start of request processing
- **`postHandle`** — logs the end of request processing including the response status

In addition, each controller logs a structured `http_request` record immediately on entry, adding:

| Field | Source |
| --- | --- |
| `log_record_name` | `"http_request"` |
| `call_type` | `"synchronous"` or `"asynchronous"` |
| `client_ip` | `HttpServletRequest.getRemoteAddr()` |
| `http_referer` | `request.getHeader("referer")` — `"null"` if absent |
| `context_id` | Derived correlation key (`applicationId-responseId` or the supplied `contextId`) |
| `response_id` | Caller-supplied |
| `original_filename` | From `MultipartFile` (sync endpoint only) |
| `file_content_type` | From `MultipartFile` (sync endpoint only) |
| `image_endpoint` | Caller-supplied URL (async endpoint only) |
| `converted_text_endpoint` | Caller-supplied URL (async endpoint only) |

### Named Log Record Types

All structured log records carry a `log_record_name` field to enable targeted log queries. The defined record names are:

| `log_record_name` value | Emitted by | When |
| --- | --- | --- |
| `http_request` | `AbstractOcrApiController.logClientRequest()` | On every inbound request, before processing |
| `post_queue` | `OcrRequestService` | When a thread picks up work from the executor queue |
| `ocr_monitoring` | `MonitoringService` | Once per completed transaction (success or failure) |
| `ocr_statistics` | `StatisticsController` / `HealthCheckController` | On each call to `/statistics` or `/healthcheck` |
| `thread_pool_executor_config` | `ThreadConfig` | On application startup when the thread pool is created |
| `low_confidence` | `ImageOcrService` | Per text line whose confidence is ≤ `LOW_CONFIDENCE_SCORE_TO_LOG` |

### Correlation / Tracing

Requests are correlated through a `contextId` string derived at the controller boundary:

- **Async:** `contextId = applicationId + '-' + responseId` (derived in `OcrRequest`)
- **Sync:** caller-supplied `contextId` parameter; falls back to `responseId` if blank

The `contextId` is threaded through every log call (`LOG.infoContext()`, `LOG.debugContext()`, `LOG.errorContext()`) and is included in the `ExtractTextResultDto` response and all error payloads, enabling end-to-end trace reconstruction across the async callback pattern.

### Startup Configuration Log

On startup, `SpringConfiguration.displaySpringProperties()` logs all resolved property values at `INFO` level, confirming the active runtime configuration:

```
Displaying spring application.properties
ocr.queue.capacity          : 10
low.confidence.to.log       : 50
rest.client.timeout.seconds : 300
host.white.list.values      : internal-service.example.com
tessdata.path               : /usr/share/tesseract/tessdata
```

> ⚠️ The `host.white.list` value is a security boundary. See `security-review.md` finding #5.

### Log Output Destination

| Environment | Destination |
| --- | --- |
| Local / Docker | `stdout` (console) |
| ECS (all environments) | CloudWatch Logs via the ECS log driver, grouped per service and environment |

---

# Monitoring

There is no external APM agent (no Datadog, no New Relic, no Micrometer). Operational monitoring is achieved entirely through **structured log events** parsed downstream in CloudWatch.

### Per-Transaction Monitoring Record

`MonitoringService` emits exactly one `ocr_monitoring` log record per OCR transaction, whether the outcome is success, failure, or partial failure (e.g. OCR succeeded but callback failed). This record is the primary data source for operational dashboards.

Fields in every `ocr_monitoring` record:

| Field | Type | Description |
| --- | --- | --- |
| `log_record_name` | String | `"ocr_monitoring"` |
| `call_type` | String | `"synchronous"` or `"asynchronous"` |
| `result_code` | int | `0` = success; see `OcrRequestException.ResultCode` for error values |
| `average_confidence_score` | int | Rounded average Tesseract line confidence across the document (0–100) |
| `lowest_confidence_score` | int | Rounded minimum Tesseract line confidence across the document (0–100) |
| `ocr_processing_time_ms` | long | Time spent in the Tesseract engine (ms) |
| `total_processing_time_ms` | long | Full end-to-end time including queue wait (ms) |
| `time_on_queue_ms` | long | Time the request spent waiting in the executor thread pool queue (ms) |
| `total_pages` | int | Number of pages processed in the TIFF |
| `file_size` | int | Input image size in bytes |

### Thread Pool Metrics

The `/ocr-api/statistics` endpoint (and every call to `/ocr-api/healthcheck`) emits an `ocr_statistics` log record with live thread pool state. This enables passive capacity monitoring without polling an external metrics endpoint:

| Field | Description |
| --- | --- |
| `instance_uuid` | Per-instance UUID (generated at startup); identifies individual ECS tasks |
| `queue_size` | Requests currently waiting in the executor queue |
| `pool_size` | Current thread count |
| `active_pool_size` | Threads actively processing OCR |
| `largest_pool_size` | Peak thread count since startup |

### Low-Confidence Alerting

Lines where Tesseract's confidence score falls at or below the `LOW_CONFIDENCE_SCORE_TO_LOG` threshold are written to a `low_confidence` record at `DEBUG` level:

| Field | Description |
| --- | --- |
| `page_number` | 1-based page number within the TIFF |
| `line_number` | Line number within the page |
| `confidence_value` | Tesseract confidence float (0–100) |
| `converted_text` | The extracted text for that line |

These records allow post-hoc identification of poor-quality scans and can be aggregated in CloudWatch to surface documents with systematically low OCR accuracy.

### Error Monitoring

All failure paths in `OcrRequestService` and the controllers call `MonitoringService.logFailure()` with the appropriate `ResultCode`. This ensures every failed transaction — including async callback failures — produces an `ocr_monitoring` record with a non-zero `result_code`, even when no HTTP error is returned to the caller (as in the async callback failure case).

### Slack Alerting

Build and deployment failures are routed to Slack via the Concourse CI pipeline:

| Channel | Events |
| --- | --- |
| `#pa_build_failures` | Build, test, PR analysis, cidev/staging deploy failures |
| `#pa_security_checks` | Weekly dependency CVE and SonarQube failures |
| `#deployments` | Live deploy failures and **successful live deploys** |

---

# Telemetry & Custom Solutions

The application has no dedicated telemetry agent or APM SDK. The following observability mechanisms are present:

### CloudWatch (Production)

Structured JSON log output from the ECS containers is collected by the ECS log driver and shipped to **AWS CloudWatch Logs**. CloudWatch Logs Insights can be used to query `log_record_name = "ocr_monitoring"` records for SLA reporting, confidence trend analysis, and error rate tracking.

### OpenTelemetry Semconv (Transitive Dependency)

The `io.opentelemetry.semconv:opentelemetry-semconv:1.41.1` library appears as a **transitive dependency** pulled in by `structured-logging`. It is not instrumented directly by the application — no `@WithSpan`, no `Tracer`, no `MeterRegistry` — but its presence means the structured logging library may emit OTel-compatible attribute names in its output.

### SonarQube (Code Quality)

Static analysis runs on every PR (`analyse-pull-request`) and weekly (`weekly-sonar-analysis`) via the Concourse pipeline. The project is registered as `uk.gov.companieshouse:ocr-api`. Results are surfaced in the CH SonarQube instance.

### Dependency Track (SBOM)

A **CycloneDX SBOM** is generated after every application release (`build-release` job) and submitted to Dependency Track under project name `ocr-api`. This provides continuous software composition analysis and CVE monitoring at the dependency level outside of the runtime application.

### Concourse Pipeline Visibility

The Concourse pipeline provides operational visibility into build and deployment state. All jobs emit pass/fail status and the `cidev-api-tests` job publishes a Karate test report to `ecs-ocr-api-service-karate-cidev` after every cidev deployment.

---

# Example Log Messages

The following illustrate the shape of structured log output for key events.

### Request received (async endpoint)

```json
{
  "log_record_name": "http_request",
  "call_type": "asynchronous",
  "client_ip": "10.0.1.45",
  "http_referer": "null",
  "applicationId": "document-service",
  "responseId": "abc-123",
  "imageEndpoint": "https://internal-docs.example.com/files/abc-123/image",
  "convertedTextEndpoint": "https://internal-docs.example.com/files/abc-123/ocr-result",
  "contextId": "document-service-abc-123",
  "timeRequestReceived": "2026-06-09T10:32:01.452"
}
```

### Post-queue — work started on thread

```json
{
  "log_record_name": "post_queue",
  "call_type": "asynchronous",
  "thread_name": "ocr-request-thread-2",
  "time_on_queue_ms": 142
}
```

### Low confidence line

```json
{
  "log_record_name": "low_confidence",
  "page_number": 3,
  "line_number": 17,
  "confidence_value": 38.4,
  "converted_text": "Artlcles 0f Ass0ciation"
}
```

### OCR monitoring record — success

```json
{
  "log_record_name": "ocr_monitoring",
  "call_type": "asynchronous",
  "result_code": 0,
  "average_confidence_score": 84,
  "lowest_confidence_score": 38,
  "ocr_processing_time_ms": 4821,
  "total_processing_time_ms": 5103,
  "time_on_queue_ms": 142,
  "total_pages": 12,
  "file_size": 4194304
}
```

### OCR monitoring record — failure (image fetch failed)

```json
{
  "log_record_name": "ocr_monitoring",
  "call_type": "asynchronous",
  "result_code": 1,
  "average_confidence_score": 0,
  "lowest_confidence_score": 0,
  "ocr_processing_time_ms": 0,
  "total_processing_time_ms": 312,
  "time_on_queue_ms": 89,
  "total_pages": 0,
  "file_size": 0
}
```

### Thread pool configuration (startup)

```json
{
  "log_record_name": "thread_pool_executor_config",
  "queue_capacity": 10,
  "max_pool_size": 4,
  "thread_name_prefix": "ocr-request-thread-",
  "processors_size": 4
}
```

### Statistics (per healthcheck or `/statistics` call)

```json
{
  "log_record_name": "ocr_statistics",
  "instance_uuid": "a3f2c1d4-7e8b-4f2a-9c1d-2e3f4a5b6c7d",
  "queue_size": 0,
  "active_pool_size": 1,
  "pool_size": 4,
  "largest_pool_size": 4
}
```
