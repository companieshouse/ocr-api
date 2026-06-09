# Configuration and Environment Variables — ocr-api

> Runtime configuration is injected entirely through environment variables. There are no hardcoded secrets or environment-specific config files inside the application JAR. Spring Boot resolves `${VAR_NAME}` placeholders in `application.properties` at startup; any missing required variable will cause the application to fail to start with a `BeanCreationException`.

---

| Variable Name | Purpose / Controls | Required / Optional | Where Defined / Loaded | Default / Format / Notes | Env-specific? |
| --- | --- | --- | --- | --- | --- |
| `OCR_QUEUE_CAPACITY` | Maximum number of OCR requests that can wait in the thread pool executor queue before the service starts rejecting requests with `HTTP 503` | ✅ Required | `application.properties` → `ocr.queue.capacity`; consumed by `ThreadConfig.ocrRequestTaskExecutor()` via `SpringConfiguration.getOcrQueueCapacity()` | No default — missing value causes startup failure. Integer. Size the queue to accommodate burst traffic beyond the thread pool size. | Yes — higher values appropriate for production; lower for dev/test |
| `LOW_CONFIDENCE_SCORE_TO_LOG` | Tesseract confidence threshold (0–100). Any text line whose OCR confidence is at or below this value is written to the debug log with its page number, line number, and text content | ✅ Required | `application.properties` → `low.confidence.to.log`; consumed by `ImageOcrService` via `SpringConfiguration.getLowConfidenceToLog()` | No default — missing value causes startup failure. Integer, range 0–100. A value of `50` means lines with ≤50% confidence are flagged. | Yes — can be tuned per environment for log verbosity |
| `HOST_WHITE_LIST` | Comma-separated list of hostnames permitted as values for `image_endpoint` and `converted_text_endpoint` in async OCR requests. The primary SSRF protection control. | ✅ Required | `application.properties` → `host.white.list`; consumed by `UrlHostValidator` via `SpringConfiguration.getHostWhiteList()` | No default — missing value causes startup failure. Format: `host1.example.com,host2.example.com` (no spaces, no schemes, no paths). Logged at `INFO` level on startup. | Yes — different internal service hostnames per environment |
| `TESSDATA_PREFIX` | Filesystem path to the directory containing Tesseract language model files (`.traineddata`). Passed directly to `TessBaseAPIInit3()`. | ✅ Required | `application.properties` → `tessdata.path`; consumed by `ImageOcrService` via `SpringConfiguration.getTessdataPath()`. Also set as an `ENV` in `ecs-image-build/Dockerfile` to `/usr/share/tesseract/tessdata` | No default — missing value causes startup failure. Must be a valid directory path accessible by the container. Dev image uses `/usr/share/tessdata`; ECS image uses `/usr/share/tesseract/tessdata`. | Yes — path differs between dev and ECS images |
| `OCR_TESSERACT_THREAD_POOL_SIZE` | Number of worker threads in the OCR executor thread pool. Controls how many TIFF files can be processed concurrently. Each thread drives one Tesseract instance. | ❌ Optional | Read directly from the OS environment by `ThreadConfig` using `EnvironmentReaderImpl.getOptionalInteger()` at bean construction time — **not** via `application.properties` | Default: `4`. Integer. Set `OMP_THREAD_LIMIT=1` separately to prevent each Tesseract instance from spawning its own threads internally. | Yes — tune to available vCPUs; ECS task is allocated 2 vCPUs by default |
| `OMP_THREAD_LIMIT` | Limits the number of OpenMP threads that the native Tesseract C library is allowed to spawn internally. Prevents each concurrent Tesseract instance from creating additional threads and causing CPU contention. | ✅ Required (for correct operation) | Set as `ENV` in both `Dockerfile` (line 30) and `ecs-image-build/Dockerfile` (line 13) | Hardcoded to `1` in both Dockerfiles. Must remain `1` when running multiple concurrent OCR threads. Changing this without understanding the threading model will cause CPU saturation. | No — always `1` |
| `LC_ALL` | Locale setting for the container. Controls character encoding behaviour in the Tesseract native library and system tools. | ✅ Required (for correct operation) | Set as `ENV` in both `Dockerfile` (line 35) and `ecs-image-build/Dockerfile` (line 15) | Hardcoded to `C` in both Dockerfiles. Required for consistent UTF-8 text extraction by Tesseract. | No — always `C` |
| `PORT` | HTTP port the Spring Boot embedded Tomcat listens on. | ✅ Required (ECS image) | Passed as `-Dserver.port="${PORT}"` in `ecs-image-build/docker_start.sh` (line 7). Also set in `terraform/groups/ecs-service/locals.tf` task environment as `{ "name": "PORT", "value": 8080 }`. | Hardcoded to `8080` in `docker_start.sh`. The ECS task exposes container port `8080` (`container_port = 8080` in `locals.tf`). The dev `Dockerfile` uses `EXPOSE 8080` without passing `PORT` to the JVM (Spring Boot defaults to `8080`). | No — always `8080` |
| `JAVA_OPTS` | Allows arbitrary JVM arguments to be injected at container start in the **development Dockerfile only**. | ❌ Optional | Set as `ENV JAVA_OPTS=""` in `Dockerfile` (line 47); expanded in the `ENTRYPOINT` shell command on line 48. Not present in `ecs-image-build/docker_start.sh`. | Default: empty string. Examples: `-Xmx2g -Xms512m`. ⚠️ Not used in the ECS production image — `docker_start.sh` does not reference `JAVA_OPTS`. | Dev only — not applicable to ECS |
| `CODE_ANALYSIS_TOKEN` | SonarQube authentication token used when running static analysis via `mvn sonar:sonar`. | ❌ Optional (CI/CD only) | Referenced in `pom.xml` (line 41) as `<sonar.token>${CODE_ANALYSIS_TOKEN}</sonar.token>`. Set in CI environment. | Not required at application runtime — only needed when running Sonar analysis. | CI/CD only |

---

## Terraform Deployment Variables

These are Terraform input variables used at infrastructure provisioning time. They are **not** runtime environment variables for the application itself, but they control how the service is deployed.

| Variable Name | Purpose | Default | Required |
| --- | --- | --- | --- |
| `environment` | Deployment environment name (e.g. `development`, `staging`, `live`) | — | ✅ |
| `aws_region` | AWS region for the ECS deployment | `eu-west-2` | ❌ |
| `aws_profile` | AWS CLI profile used during Terraform apply | `development-eu-west-2` | ❌ |
| `docker_registry` | FQDN of the ECR registry hosting the container image | — | ✅ |
| `ocr_api_version` | Tag of the `ocr-api` container image to deploy | — | ✅ |
| `desired_task_count` | ECS desired task count | `1` | ❌ |
| `max_task_count` | ECS maximum task count for autoscaling | `1` | ❌ |
| `min_task_count` | ECS minimum task count | `1` | ❌ |
| `required_cpus` | ECS task CPU units (1024 = 1 vCPU) | `2048` (2 vCPU) | ❌ |
| `required_memory` | ECS task memory (MB) | `7680` (7.5 GB) | ❌ |
| `service_autoscale_enabled` | Enable ECS service autoscaling | `true` | ❌ |
| `service_autoscale_target_value_cpu` | CPU % target for autoscaling trigger | `80` | ❌ |
| `cloudwatch_alarms_enabled` | Create standard CloudWatch alarms | `true` | ❌ |
| `hashicorp_vault_username` | Username for Vault authentication (userpass method) | — | ✅ |
| `hashicorp_vault_password` | Password for Vault authentication | — | ✅ |
| `use_set_environment_files` | Toggle global/shared environment file injection into ECS task | `true` | ❌ |

---

## Additional Notes

### How configuration is loaded at startup

At application startup, `SpringConfiguration` reads all `@Value`-injected properties and logs every resolved value at `INFO` level via its `@PostConstruct` method (`SpringConfiguration.java:31`). This provides an immediate operational confirmation of the active configuration but also means the `HOST_WHITE_LIST` value (a security boundary) is written to the application logs. See `security-review.md` finding #5.

### Required vs Optional — startup behaviour

The four properties backed by environment variables in `application.properties` (`OCR_QUEUE_CAPACITY`, `LOW_CONFIDENCE_SCORE_TO_LOG`, `HOST_WHITE_LIST`, `TESSDATA_PREFIX`) have **no defaults defined**. If any is absent, Spring Boot will throw a `BeanCreationException` during context initialisation and the service will not start. The ECS task and dev container must have all four set.

`OCR_TESSERACT_THREAD_POOL_SIZE` is the exception — it is read via `EnvironmentReaderImpl.getOptionalInteger()` in `ThreadConfig.java:39` and defaults to `4` if absent.

### Tessdata path differences between images

| Image | `TESSDATA_PREFIX` value | Data source |
| --- | --- | --- |
| Dev `Dockerfile` | `/usr/share/tessdata` | Copied from `docker-resources/tessdata/2020-10-21/eng.traineddata` at build time |
| ECS `ecs-image-build/Dockerfile` | `/usr/share/tesseract/tessdata` | Installed by `dnf install tesseract tesseract-langpack-eng` at image build time |

The tessdata path must be consistent with the image being run. Mixing images and path variables will cause Tesseract to fail to initialise with a data path error.

### Secret management in production

Application secrets and environment-specific configuration are stored in **HashiCorp Vault** and retrieved during Terraform provisioning. They are written to **AWS SSM Parameter Store** (encrypted with a KMS key) and injected into the ECS task definition as `secrets` (resolved at task launch time). This means no secrets appear in plaintext in the task definition or Terraform state. The `module.secrets` block in `main.tf:21` manages this flow.

### Thread pool sizing guidance

The default pool size of `4` threads was chosen to match typical vCPU counts. Since `OMP_THREAD_LIMIT=1` pins each Tesseract instance to a single OS thread, the effective CPU utilisation is approximately `OCR_TESSERACT_THREAD_POOL_SIZE` cores. The ECS task default of 2 vCPUs means a pool size of `2` is the safe production default; setting it higher than the available vCPUs will cause CPU contention and increased latency. The `OCR_QUEUE_CAPACITY` should be set high enough to absorb burst traffic without immediately returning `503`, but not so high that memory is exhausted by buffered image bytes.

### REST client timeout

The `rest.client.timeout.seconds` property (line 11 of `application.properties`) controls both the connect and read timeout on the `RestTemplate` used for all outbound HTTP calls (image fetch and callback POST). The default value in `application.properties` is `300` seconds (5 minutes). This is deliberately generous to accommodate large TIFF downloads over slow internal links, but it means a hung upstream service can hold a thread for up to 5 minutes. This value is not backed by an environment variable and must be changed at the `application.properties` level if a shorter timeout is required.
