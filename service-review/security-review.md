# Security Audit — ocr-api

**Date:** 2026-06-09
**Scope:** Full application source code, configuration, Dockerfiles, and Terraform
**Application:** Spring Boot 4.0.6 OCR API (Java 21)

---

## Summary

The `ocr-api` is an internal microservice that accepts images (TIFF), performs OCR via Tesseract, and returns or callbacks with extracted text. The application has **no authentication or authorisation layer** and relies on network-level controls (load balancer path rules, VPC placement) for access control. Several findings are detailed below, ranked by risk and confidence.

---

## Findings

### 1. 🔴 No Authentication or Authorisation (HIGH / HIGH Confidence)

**Description:** The application has no `spring-boot-starter-security` dependency, no security configuration, and no authentication mechanism of any kind. All endpoints (`POST /ocr-api/api/ocr/image/tiff/extractTextRequest`, `POST /ocr-api/api/ocr/image/tiff/extractText`, `GET /ocr-api/healthcheck`, `GET /ocr-api/statistics`) are publicly accessible to anyone who can reach the service on the network.

The Terraform configuration (`main.tf` line 86) explicitly disables the ERIC reverse proxy (`use_eric_reverse_proxy = false`), which appears to be a Companies House API gateway/auth layer:

```terraform
# terraform/groups/ecs-service/main.tf:86
use_eric_reverse_proxy    = false
```

**Impact:** Any actor with network access to the service can submit OCR requests, consume processing resources, retrieve internal service statistics (including the instance UUID), and potentially use the service as a proxy to make outbound HTTP requests (see SSRF finding below).

**Recommendation:**
- Enable the ERIC reverse proxy or integrate `spring-boot-starter-security` with appropriate API key / OAuth2 / mTLS authentication.
- Apply the principle of least privilege—restrict endpoints to only authorised callers.

---

### 2. 🔴 Server-Side Request Forgery (SSRF) via Async Endpoint (HIGH / HIGH Confidence)

**Description:** The async endpoint accepts user-supplied URLs (`image_endpoint` and `converted_text_endpoint`) in the JSON request body. While there is a hostname whitelist check (`UrlHostValidator`), there are multiple weaknesses:

1.  **Deprecated `java.net.URL` constructor** is used for URL parsing (`UrlHostValidator.java` line 46), which has known parsing inconsistencies and can be confused by certain URL formats (e.g. `http://whitelisted-host@attacker.com`).

    ```java
    // src/main/java/uk/gov/companieshouse/ocr/api/urlvalidator/UrlHostValidator.java:46
    var url = new URL(urlString);
    ```

2.  **No scheme restriction**: The validator uses `UrlValidator.ALLOW_LOCAL_URLS` (line 30) but doesn't restrict the scheme to only `https`. This allows `http://` and potentially other schemes.

    ```java
    // src/main/java/uk/gov/companieshouse/ocr/api/urlvalidator/UrlHostValidator.java:30
    var validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
    ```

3.  **No IP address validation**: The whitelist checks hostnames only, but doesn't prevent DNS rebinding attacks where a whitelisted hostname's DNS record is changed to resolve to an internal IP.

4.  **The `RestTemplate` then makes outbound HTTP calls** to whatever endpoint was supplied (`ImageRestClient.java` line 31 and `CallbackExtractedTextRestClient.java` line 35), meaning a successful bypass results in arbitrary HTTP GET and POST requests from the server.

    ```java
    // src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ImageRestClient.java:31
    return restTemplate.getForEntity(imageEndpoint, byte[].class).getBody();
    ```

    ```java
    // src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/CallbackExtractedTextRestClient.java:35
    restTemplate.postForEntity(extractedTextEndpoint, entity, String.class);
    ```

**Impact:** An attacker could exploit this to scan internal services, access metadata endpoints (e.g. AWS EC2 instance metadata at `http://169.254.169.254/`), or exfiltrate data to external systems.

**Recommendation:**
- Replace `java.net.URL` with `java.net.URI` for parsing.
- Validate the **resolved IP address** of the hostname before making the request to prevent DNS rebinding and TOCTOU attacks.
- Restrict allowed schemes to `https` only.
- Consider validating URLs against a stricter allowlist that includes path prefixes, not just hostnames.
- Block requests to private/loopback IP ranges (10.x, 172.16-31.x, 192.168.x, 127.x, 169.254.x, etc.).

---

### 3. 🟠 Denial of Service via Large File Uploads (MEDIUM / HIGH Confidence)

**Description:** The application allows file uploads up to **100MB** per request:

```ini
# src/main/resources/application.properties:8-9
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

OCR processing is CPU-intensive, and the thread pool is bounded (default 4 threads, configurable):

```java
// src/main/java/uk/gov/companieshouse/ocr/api/ThreadConfig.java:25
private static final int DEFAULT_THREAD_POOL_SIZE = 4;
```

While there is a `TaskRejectedException` handler when the queue is full, an attacker can still exhaust resources by submitting the maximum number of large files that saturate the queue and thread pool.

**Impact:** Service degradation or denial of service for legitimate users. The 100MB limit combined with multi-page TIFF processing can consume significant CPU and memory.

**Recommendation:**
- Reduce the maximum file size to the minimum required for the use case.
- Implement rate limiting at the API gateway or application level.
- Consider adding a maximum page count check in `ImageOcrService.extractTextFromImageViaApi()` to prevent processing documents with excessive pages.

---

### 4. 🟠 Information Disclosure via Statistics and Health Endpoints (MEDIUM / HIGH Confidence)

**Description:** The `/ocr-api/statistics` endpoint is unauthenticated and exposes internal operational data:

```java
// src/main/java/uk/gov/companieshouse/ocr/api/statistics/StatisticsService.java:15
private static final String INSTANCE_UUID = UUID.randomUUID().toString();
```

The response includes: `instance_uuid`, `active_pool_size`, `largest_pool_size`, `pool_size`, and `queue_size`. This gives an attacker insight into the internal state of the application, including how loaded it is and a unique instance identifier.

**Impact:** Aids an attacker in fingerprinting the deployment, planning DoS attacks (knowing when the queue is near capacity), and correlating requests to specific instances.

**Recommendation:**
- Protect the statistics and healthcheck endpoints behind authentication or restrict access to internal monitoring tools only.

---

### 5. 🟠 Sensitive Configuration Logged at Startup (MEDIUM / HIGH Confidence)

**Description:** The `SpringConfiguration.displaySpringProperties()` method logs all configuration values at startup, including the host whitelist:

```java
// src/main/java/uk/gov/companieshouse/ocr/api/SpringConfiguration.java:37
LOG.info("The value of ${host.white.list.values} is      :     " + hostWhiteList);
```

**Impact:** The host whitelist values are security-sensitive (they define the SSRF protection boundary). If logs are accessible to a wider audience or are shipped to a centralised logging system without access controls, this could help an attacker understand which hosts to target for SSRF bypass.

**Recommendation:**
- Avoid logging security-sensitive configuration values, or redact/mask them.
- Log at `DEBUG` level rather than `INFO` if this is needed for development troubleshooting only.

---

### 6. 🟠 User-Supplied Data Logged Without Sanitisation (MEDIUM / MEDIUM Confidence)

**Description:** Several places log user-supplied input directly, which could enable log injection or log forging attacks:

```java
// src/main/java/uk/gov/companieshouse/ocr/api/urlvalidator/UrlHostValidator.java:28
LOG.debug("Validating URL: " + url);
```

```java
// src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ImageRestClient.java:28
LOG.debugContext(contextId, "Image from [" + imageEndpoint + "]",null);
```

```java
// src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/AbstractOcrApiController.java:43
requestMap.put(JsonConstants.HTTP_REFERER_NAME, request.getHeader("referer") != null ? request.getHeader("referer") : "null");
```

The `referer` header, URLs, context IDs, and other client-supplied values are logged without sanitisation.

**Impact:** An attacker could inject newline characters or structured log data to forge log entries, potentially misleading incident responders or injecting false audit trail entries.

**Recommendation:**
- Sanitise all user-supplied values before logging (strip newlines, control characters).
- Since the application uses structured logging, ensure values are placed in structured fields rather than interpolated into message strings.

---

### 7. 🟡 No Content-Type Validation on File Uploads (LOW-MEDIUM / HIGH Confidence)

**Description:** The synchronous endpoint accepts a `MultipartFile` but performs no validation on the file's content type or magic bytes:

```java
// src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/SyncImageOcrApiController.java:53-58
public @ResponseBody ResponseEntity<ExtractTextResultDto> extractTextFromTiff(
        @RequestParam(FILE_REQUEST_PARAMETER_NAME) MultipartFile file,
        ...
```

The file is directly passed to `file.getBytes()` and then to the Tesseract OCR engine. While the `ImageReader` will fail for unsupported formats, there is no early validation.

**Impact:** Maliciously crafted image files could potentially exploit vulnerabilities in image parsing libraries (ImageIO, Tesseract, PDFBox). Without content-type validation, the attack surface is broader.

**Recommendation:**
- Validate the file's content type (MIME type) against an allowlist (e.g. `image/tiff`).
- Validate file magic bytes (file header) to confirm the content matches the expected format.
- Keep image processing libraries (tess4j, PDFBox, ImageIO) updated.

---

### 8. 🟡 Dockerfile Runs as Root (LOW-MEDIUM / HIGH Confidence)

**Description:** The development `Dockerfile` does not specify a non-root user:

```dockerfile
# Dockerfile:48
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
```

There is no `USER` directive, so the application runs as `root` inside the container.

*Note:* The ECS production Dockerfile (`ecs-image-build/Dockerfile`) uses a base image that may set a non-root user, but this should be explicitly verified.

**Impact:** If an attacker achieves code execution (e.g. via an image parsing vulnerability), running as root gives them full control of the container, making container escapes easier.

**Recommendation:**
- Add a non-root user to the Dockerfile and use the `USER` directive:
  ```dockerfile
  RUN groupadd -r appuser && useradd -r -g appuser appuser
  USER appuser
  ```

---

### 9. 🟡 No TLS Enforcement for Outbound HTTP Calls (LOW-MEDIUM / MEDIUM Confidence)

**Description:** The `RestTemplate` used for outbound calls (`ImageRestClient`, `CallbackExtractedTextRestClient`) does not enforce HTTPS. The URL scheme depends entirely on what the client provides (filtered only by hostname whitelist). The `RestTemplateConfig` does not configure SSL/TLS settings:

```java
// src/main/java/uk/gov/companieshouse/ocr/api/RestTemplateConfig.java:14-18
return restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(springConfiguration.getRestClientTimeoutSeconds()))
        .readTimeout(Duration.ofSeconds(springConfiguration.getRestClientTimeoutSeconds()))
        .build();
```

**Impact:** If HTTP (not HTTPS) URLs are used, data including OCR results (potentially containing sensitive document text) could be transmitted in cleartext and intercepted by a network-level attacker.

**Recommendation:**
- Enforce HTTPS-only for outbound calls.
- Validate that all outbound URLs use the `https://` scheme.

---

### 10. 🟡 `ALLOW_LOCAL_URLS` Enabled in URL Validator (LOW-MEDIUM / HIGH Confidence)

**Description:** The URL validator explicitly permits local URLs:

```java
// src/main/java/uk/gov/companieshouse/ocr/api/urlvalidator/UrlHostValidator.java:30
var validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
```

This means URLs with hostnames like `localhost`, `127.0.0.1`, or other local addresses are considered valid by the Apache Commons `UrlValidator`. Although the hostname whitelist provides a secondary check, the combination increases SSRF risk if the whitelist ever includes a hostname that resolves locally.

**Impact:** Increases the attack surface for SSRF if whitelist configuration is misconfigured.

**Recommendation:**
- Remove `ALLOW_LOCAL_URLS` unless there is a documented requirement for it.
- Explicitly block private/loopback IP ranges at the IP validation level.

---

### 11. 🟡 Stale Tesseract Training Data (LOW / HIGH Confidence)

**Description:** The development Dockerfile bundles tessdata from **2020-10-21**:

```dockerfile
# Dockerfile:16
COPY ../docker-resources/tessdata/2020-10-21/eng.traineddata /usr/share/tessdata/eng.traineddata
```

**Impact:** Older training data may have unpatched processing bugs. While this is primarily a quality/accuracy issue, outdated data files in combination with the native Tesseract library could theoretically introduce stability or security issues when processing adversarial inputs.

**Recommendation:**
- Update to the latest stable tessdata release.
- Ensure the production ECS image uses a current and verified tessdata version.

---

### 12. 🟡 Verbose Error Messages Returned to Clients (LOW / MEDIUM Confidence)

**Description:** Some error messages include internal details that could be useful to an attacker:

```java
// src/main/java/uk/gov/companieshouse/ocr/api/urlvalidator/UrlHostValidator.java:33
throw new UrlValidatorException("Invalid URL: " + url);

// src/main/java/uk/gov/companieshouse/ocr/api/urlvalidator/UrlHostValidator.java:37
throw new UrlValidatorException("URL not on white list: " + url);
```

The exception handler in `AbstractOcrApiController` returns `ore.getResultCode().getMessage()` which includes messages like "Bad Url contained in OCR Request". While the most sensitive error details are not directly returned, the error responses confirm the existence of the whitelist mechanism.

**Impact:** Low — reveals defensive mechanisms to an attacker. The whitelist confirmation could help attackers refine their SSRF payloads.

**Recommendation:**
- Return generic error messages to clients (e.g. "Invalid request").
- Log detailed error information server-side only.

---

### 13. 🟢 `JAVA_OPTS` Injectable via Environment Variable (LOW / MEDIUM Confidence)

**Description:** The Dockerfile ENTRYPOINT uses `$JAVA_OPTS` in a shell-evaluated string:

```dockerfile
# Dockerfile:48
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
```

If an attacker could control the `JAVA_OPTS` environment variable (e.g. via a container orchestration misconfiguration), they could inject arbitrary JVM arguments including agent loading or remote debugging.

**Impact:** Low — requires prior access to the container environment configuration.

**Recommendation:**
- In the ECS Dockerfile (`docker_start.sh`), the `JAVA_OPTS` pattern is not used, which is better. Ensure the development Dockerfile is not used in production.
- If `JAVA_OPTS` is needed, validate or constrain its contents.

---

### 14. 🟢 No Rate Limiting (LOW / HIGH Confidence)

**Description:** There is no rate limiting at the application level. The only throttling mechanism is the bounded thread pool queue, which returns `HTTP 503` when full. There is no per-client rate limiting.

**Impact:** Enables abuse by a single client that can flood the service with requests.

**Recommendation:**
- Implement rate limiting at the API gateway level or within the application using a library like Bucket4j or Spring Cloud Gateway rate limiter.

---

## Dependency CVE Status

As of the audit date, the following key dependencies were checked and **no active CVEs were found**:
- `org.springframework.boot:spring-boot-starter-web@4.0.6`
- `net.sourceforge.tess4j:tess4j@5.18.0`
- `org.json:json@20240303`
- `org.apache.tomcat.embed:tomcat-embed-core@11.0.22`
- `org.apache.pdfbox:pdfbox@3.0.7`
- `org.apache.commons:commons-lang3@3.20.0`

The project already has CVE override versions in `pom.xml` (lines 78–121) addressing multiple historical CVEs in Tomcat, PDFBox, and OpenTelemetry.

---

## Risk Summary Table

| # | Finding | Severity | Confidence | Category |
|---|---------|----------|------------|----------|
| 1 | No Authentication/Authorisation | 🔴 HIGH | HIGH | Access Control |
| 2 | SSRF via user-supplied URLs | 🔴 HIGH | HIGH | Injection |
| 3 | DoS via large file uploads | 🟠 MEDIUM | HIGH | Availability |
| 4 | Information disclosure via stats endpoint | 🟠 MEDIUM | HIGH | Information Disclosure |
| 5 | Sensitive config logged at startup | 🟠 MEDIUM | HIGH | Information Disclosure |
| 6 | Log injection via unsanitised input | 🟠 MEDIUM | MEDIUM | Injection |
| 7 | No content-type validation on uploads | 🟡 LOW-MED | HIGH | Input Validation |
| 8 | Docker container runs as root | 🟡 LOW-MED | HIGH | Configuration |
| 9 | No TLS enforcement for outbound calls | 🟡 LOW-MED | MEDIUM | Transport Security |
| 10 | `ALLOW_LOCAL_URLS` in URL validator | 🟡 LOW-MED | HIGH | Configuration |
| 11 | Stale tessdata from 2020 | 🟡 LOW | HIGH | Maintenance |
| 12 | Verbose error messages | 🟡 LOW | MEDIUM | Information Disclosure |
| 13 | `JAVA_OPTS` injectable via env | 🟢 LOW | MEDIUM | Configuration |
| 14 | No rate limiting | 🟢 LOW | HIGH | Availability |
