# ocr-api

## What is the ocr-api?

The `ocr-api` is an internal microservice that performs **Optical Character Recognition (OCR)** on TIFF image files. It is part of the Companies House document processing pipeline, converting scanned document images (such as articles of association) into machine-readable text. The service is designed to be called by other internal services rather than directly by end users.

---

## What does the service do?

The service exposes two OCR processing modes:

### Synchronous Mode
**`POST /ocr-api/api/ocr/image/tiff/extractText`**

The caller uploads a TIFF image directly as a multipart file. The service processes the image immediately and returns the extracted text and confidence scores in the HTTP response body. Suitable for smaller documents where the caller can wait for the result.

**Request parameters:**
- `file` — the TIFF image (multipart upload, up to 100MB)
- `responseId` — caller-supplied identifier for the response
- `contextId` *(optional)* — caller-supplied context identifier for tracing; defaults to `responseId` if not provided

**Response:** JSON payload containing the extracted text, average confidence score, lowest confidence score, and processing time metrics.

### Asynchronous Mode
**`POST /ocr-api/api/ocr/image/tiff/extractTextRequest`**

The caller submits a JSON body containing URLs for where the service should fetch the image and where it should deliver the result. The service immediately returns `HTTP 202 Accepted` and processes the request in the background. When processing is complete (or on error), the service POSTs the result back to the `converted_text_endpoint` URL provided by the caller.

**Request body fields:**
- `app_id` — identifier of the calling application
- `response_id` — caller-supplied identifier for the response
- `image_endpoint` — URL the service will fetch the TIFF image from
- `converted_text_endpoint` — URL the service will POST the OCR result back to

Both modes produce the same result payload:

| Field | Description |
|---|---|
| `extracted_text` | The full text extracted from the document |
| `average_confidence_score` | Average Tesseract confidence across all lines (0–100) |
| `lowest_confidence_score` | Lowest per-line confidence score (0–100) |
| `ocr_processing_time_ms` | Time taken for the OCR conversion itself |
| `total_processing_time_ms` | Total end-to-end time for the request |
| `response_id` | Echo of the caller-supplied response ID |
| `context_id` | Echo of the context ID |
| `result_code` | `0` = success, positive integer = specific error code |

### Supporting Endpoints

| Endpoint | Description |
|---|---|
| `GET /ocr-api/healthcheck` | Returns `200 OK` with the text `ocr-api is alive` when the service is running |
| `GET /ocr-api/statistics` | Returns internal thread pool metrics (queue size, pool size, active threads, instance UUID) |

### OCR Processing Detail

Under the hood, the service uses [Tesseract OCR](https://github.com/tesseract-ocr/tesseract) (via the `tess4j` Java wrapper) to process images. For each submitted image:

1. The TIFF is read page by page using Java's `ImageIO`.
2. Each page is passed to the Tesseract API (`TessBaseAPI`) configured with the English language model.
3. Tesseract returns the extracted UTF-8 text and a per-line confidence score (0–100).
4. Lines with a confidence score below a configurable threshold (`LOW_CONFIDENCE_SCORE_TO_LOG`) are flagged in the logs for monitoring purposes.
5. The per-page and document-level average and minimum confidence scores are computed and included in the response.

Multi-page TIFFs are fully supported; all pages are processed sequentially and their text is concatenated in the result.

---

## How is it built?

### Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| OCR Engine | Tesseract (via `tess4j` 5.18.0) |
| Build Tool | Maven |
| Container Build | Google Jib (for ECS) / Docker (for local dev) |
| Infrastructure | AWS ECS (Fargate), ALB, SSM Parameter Store, HashiCorp Vault |

### Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API layer |
| `spring-boot-starter-validation` | Jakarta Bean Validation for request DTOs |
| `spring-boot-starter-restclient` | Outbound HTTP calls (image fetch, callback) |
| `tess4j` | Java JNA wrapper for the Tesseract C library |
| `structured-logging` | Companies House structured JSON logging |
| `environment-reader-library` | Companies House environment variable utilities |
| `commons-lang3` | String utilities (`StringUtils.trim`, `StopWatch`) |
| `commons-validator` | URL validation (`UrlValidator`) |

### Concurrency Model

OCR processing is CPU-intensive and is handled off the main request threads using a dedicated `ThreadPoolTaskExecutor` (`ocrRequestTaskExecutor`). This allows the HTTP layer to remain responsive.

- **Thread pool size**: configurable via the `OCR_TESSERACT_THREAD_POOL_SIZE` environment variable; defaults to 4.
- **Queue capacity**: configurable via the `OCR_QUEUE_CAPACITY` environment variable.
- When the queue is full, the service returns `HTTP 503 Service Unavailable`.
- Tesseract's own internal threading is pinned to a single thread via `OMP_THREAD_LIMIT=1` to prevent resource contention between concurrent requests.

### Configuration

All runtime configuration is supplied via environment variables, mapped through `application.properties`:

| Environment Variable | Property | Description |
|---|---|---|
| `OCR_QUEUE_CAPACITY` | `ocr.queue.capacity` | Max requests that can queue for processing |
| `LOW_CONFIDENCE_SCORE_TO_LOG` | `low.confidence.to.log` | Confidence threshold below which lines are flagged in logs |
| `HOST_WHITE_LIST` | `host.white.list` | Comma-separated list of allowed hostnames for SSRF protection (async mode) |
| `TESSDATA_PREFIX` | `tessdata.path` | Filesystem path to Tesseract language data files |
| `OCR_TESSERACT_THREAD_POOL_SIZE` | *(read directly)* | Number of OCR worker threads |

### Building and Running

```bash
# Run unit tests
make test-unit

# Run integration tests
make test-integration

# Build the JAR (skips tests)
make build

# Build and run locally via Docker
docker build -t ocr-api .
docker run -p 8080:8080 \
  -e OCR_QUEUE_CAPACITY=10 \
  -e LOW_CONFIDENCE_SCORE_TO_LOG=50 \
  -e HOST_WHITE_LIST=internal-service.example.com \
  -e TESSDATA_PREFIX=/usr/share/tessdata \
  ocr-api
```

For local Kubernetes development with live reload, a `Tiltfile.dev` is provided that uses Google Jib to build and sync class files into the running container.

### Deployment

The service is deployed to AWS ECS (Fargate) via Terraform (under `terraform/groups/ecs-service/`). Secrets and environment configuration are stored in HashiCorp Vault and AWS SSM Parameter Store, and are injected into the ECS task at runtime. The production container image is built using the `ecs-image-build/Dockerfile` from a base Amazon Corretto 21 image, with Tesseract installed from the AL2023 package repository.

### Code Quality and Security

- Static analysis: SonarQube (`mvn sonar:sonar`)
- Test coverage: JaCoCo
- Dependency CVE scanning is performed as part of the Maven build; known CVE overrides are managed directly in `pom.xml`

---

## Where is it in the repository?

<https://github.com/companieshouse/ocr-api>
