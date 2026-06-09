# Deployment — ocr-api

---

# Build System

The project uses **Apache Maven** as its build tool, wrapped by a `Makefile` for common developer tasks. The application is packaged as a fat JAR using the `spring-boot-maven-plugin`.

### Maven Build

```bash
# Full build (skips tests)
mvn package -DskipTests=true

# Run unit tests only
mvn test -Dincluded.tests="unit-test"

# Run integration tests only
mvn test -Dincluded.tests="integration-test"

# Run all tests
mvn verify

# Static analysis (requires CODE_ANALYSIS_TOKEN env var)
mvn sonar:sonar
```

### Make Targets

| Target | Command | Description |
| --- | --- | --- |
| `make build` | `mvn versions:set … && mvn package -DskipTests=true` | Sets version, builds the fat JAR, copies it to `./ocr-api.jar` |
| `make test` | `mvn verify` | Runs the full test suite (unit + integration) |
| `make test-unit` | `mvn test -Dincluded.tests="unit-test"` | Runs unit tests only |
| `make test-integration` | `mvn test -Dincluded.tests="integration-test"` | Runs integration tests only |
| `make dev` | `mvn package` (with tests) | Builds for local development |
| `make package` | Sets version + builds + zips artifact | Produces a versioned `.zip` for release |
| `make dist` | `clean` + `build` + `package` | Full release artifact pipeline |
| `make sonar` | `mvn sonar:sonar` | Runs SonarQube analysis |
| `make clean` | `mvn clean` + removes JARs and build dirs | Cleans the workspace |

### Container Build

**Development image** — standard multi-stage `docker build` using the root `Dockerfile`. Requires the JAR to already be built:

```bash
make build
docker build -t ocr-api .
```

**Production (ECS) image** — built by Google **Jib** via the `jib-maven-plugin` (version `3.3.0`). Jib builds and pushes a container image directly to ECR without requiring a local Docker daemon. Used by CI/CD:

```bash
mvn compile jib:build -Dimage=<ECR_REGISTRY>/ocr-api:<VERSION>
```

For local Kubernetes development, Jib can build directly to the local Docker daemon:

```bash
mvn compile jib:dockerBuild -Dimage=$EXPECTED_REF
```

---

# Running Locally

### Option 1 — Docker (recommended)

Requires the JAR to be built first (`make build`). All required environment variables must be provided.

```bash
# 1. Build the JAR
make build

# 2. Build the image
docker build -t ocr-api .

# 3. Run the container
docker run -p 8080:8080 \
  -e OCR_QUEUE_CAPACITY=10 \
  -e LOW_CONFIDENCE_SCORE_TO_LOG=50 \
  -e HOST_WHITE_LIST=localhost \
  -e TESSDATA_PREFIX=/usr/share/tessdata \
  ocr-api
```

The service will be available at `http://localhost:8080`.

### Option 2 — Tilt (live reload for Kubernetes development)

The `Tiltfile.dev` provides a live-reload development loop that uses Jib to build and sync compiled classes directly into a running container without a full rebuild:

```bash
tilt up -f Tiltfile.dev
```

On each change to `src/`, Tilt runs `mvn clean compile`, syncs the updated `.class` files from `./target/classes` into the container at `/app/classes`, and restarts the container. This avoids rebuilding the full image on every code change.

### Option 3 — Run the JAR directly

Tesseract must be installed on the host machine and `TESSDATA_PREFIX` must point to a valid tessdata directory.

```bash
make build

export OCR_QUEUE_CAPACITY=10
export LOW_CONFIDENCE_SCORE_TO_LOG=50
export HOST_WHITE_LIST=localhost
export TESSDATA_PREFIX=/usr/share/tessdata

java -jar ocr-api.jar
```

---

# Service Startup

### Startup sequence

1. Spring Boot initialises the application context.
2. `SpringConfiguration` resolves all `@Value` properties from environment variables. If any required variable is missing, the context fails to start with a `BeanCreationException`.
3. `SpringConfiguration.displaySpringProperties()` (`@PostConstruct`) logs all resolved configuration values at `INFO` level.
4. `ThreadConfig` reads `OCR_TESSERACT_THREAD_POOL_SIZE` from the OS environment (defaults to `4` if absent) and creates the `ocrRequestTaskExecutor` `ThreadPoolTaskExecutor`. The pool configuration is logged.
5. `RestTemplateConfig` creates the shared `RestTemplate` with connect/read timeouts.
6. `UrlHostValidator` is initialised with the `HOST_WHITE_LIST` value split on commas.
7. Spring MVC registers all controllers and the `LoggingInterceptor`.
8. Embedded Tomcat starts on port `8080` (or the port set by `-Dserver.port`).

### ECS startup (`docker_start.sh`)

In the ECS production image, the container entry point is `ecs-image-build/docker_start.sh`:

```bash
exec java -jar \
  -Djava.security.egd=file:/dev/./urandom \
  -Dserver.port="${PORT}" \
  "ocr-api.jar"
```

`-Djava.security.egd=file:/dev/./urandom` speeds up JVM startup in containerised environments by using the non-blocking `/dev/urandom` entropy source rather than `/dev/random`.

### ECS health check grace period

The ECS service is configured with a `health_check_grace_period_seconds = 300` in `main.tf:44`. This gives the container up to 5 minutes to pass its first health check before the ALB considers it unhealthy and replaces it. This accounts for Tesseract initialisation and Spring Boot startup time on first request.

---

# CI/CD Pipeline

* **CI/CD:** [Concourse CI](https://concourse-ci.org/). The pipeline is defined in the [`companieshouse/ci-pipelines`](https://github.com/companieshouse/ci-pipelines) repository at:
  `pipelines/ssplatform/team-development/ocr-api`

The pipeline has two independent versioned tracks — one for the **application** and one for the **Terraform infrastructure** — which are released and deployed separately.

---

### Triggers & Resources

| Resource | Type | Trigger event | Notes |
| --- | --- | --- | --- |
| `source-code` | Git (`main` branch) | Push to `main`, excluding `terraform/*` | Drives build and release |
| `terraform-code` | Git (`main` branch) | Push to `main`, `terraform/*` only | Drives terraform release |
| `pull-request` | Pull request | PR opened/updated, excluding `terraform/` | Drives PR analysis |
| `pull-request-terraform` | Pull request | PR opened/updated, `terraform/` only | Drives Terraform Trivy scan |
| `release-tag` | Git tag | Tag matching `^[0-9]+\.[0-9]+\.[0-9]+$` | Gates `build-release` |
| `terraform-release-tag` | Git tag | Tag matching `^ecs-service-[0-9]+\.[0-9]+\.[0-9]+$` | Gates `terraform-release` |
| `deployment-release` | GitHub release | New release tag `^[0-9]+\.[0-9]+\.[0-9]+$` | Drives ECR build and all environment deploys |
| `weekly-cron` | Cron | Every Thursday at 22:00 UTC | Triggers weekly security jobs |

---

### Pull Request Jobs

| Job | What it does |
| --- | --- |
| `analyse-pull-request` | Runs SonarQube PR analysis using the Java 21 Corretto task. Sets GitHub PR status to pending/success/failure. Notifies `#pa_build_failures` on Slack on failure. |
| `dependency-check` | Runs an OWASP/CVE dependency check (passed `analyse-pull-request`). Posts a PR comment — `"CI: No security warnings found"` or `"CI: Security warnings found!"`. |
| `analyse-pull-request-terraform` | Runs a **Trivy** vulnerability scan against the `terraform/` directory on PRs that touch Terraform. |

---

### Main Branch Build Jobs (triggered on push to `main`)

| Job | Passed | What it does |
| --- | --- | --- |
| `build-test-analyse` | — | Builds the JAR and runs unit tests + SonarQube analysis using the Java 21 Corretto task. Notifies `#pa_build_failures` on failure. |
| `build-test-integration` | `build-test-analyse` | Runs integration tests using Testcontainers (Ryuk disabled; Docker host supplied via `DOCKER_HOST` secret). Notifies `#pa_build_failures` on failure. |

---

### Release Jobs

| Job | Trigger | What it does |
| --- | --- | --- |
| `build-release` | `source-code` push + `release-tag` present (passed `build-test-analyse`) | 1. Verifies no existing release tag exists (`verify-not-release`). 2. Calculates the new semantic version. 3. Builds and packages the JAR into a versioned `.zip` (`ocr-api-<VERSION>.zip`). 4. Uploads the ZIP to the S3 release bucket (`s3-release-bucket`). 5. Creates a GitHub release with the ZIP as an asset. 6. Generates a **CycloneDX SBOM** for dependency tracking (`DEP_TRACK_PROJ_NAME: ocr-api`). |
| `terraform-release` | `terraform-code` push + `terraform-release-tag` present | Calculates new `ecs-service-X.Y.Z` version and creates a Terraform GitHub release. |
| `docker-build-ecr` | Passed `build-release`; also triggered by new S3 artifact | 1. Downloads the versioned ZIP from S3 and unpacks it into `build-context/app/`. 2. Copies `ecs-image-build/` (the production `Dockerfile` and `docker_start.sh`) into the build context. 3. Builds the Docker image using the **ECS `Dockerfile`** (not the root `Dockerfile`). 4. Pushes the image to ECR tagged with the release version. |

---

### Environment Deploy Jobs

Each environment follows a **plan → apply** pattern using Terraform 1.3. Terraform variables (`docker_registry`, `ocr_api_version`, `eric_version`, Vault credentials) are written to a `.tfvars` file at runtime from pipeline secrets and the release tag. Terraform plans are stored as versioned ZIP files in environment-specific S3 buckets.

After each `apply`, a **15-minute ECS service readiness check** polls the ECS service until the new task version is running and stable before the pipeline proceeds.

| Job | Passed | ECS Cluster | ECS Service | Auto-triggered |
| --- | --- | --- | --- | --- |
| `cidev-plan` | `docker-build-ecr`, `terraform-release` | — | — | ✅ Yes |
| `cidev-apply` | `cidev-plan` | `ocr-cidev-cluster` | `cidev-ocr-api` | ✅ Yes |
| `cidev-api-tests` | `cidev-apply` | — | — | ✅ Yes |
| `staging-plan` | `cidev-apply` | — | — | ✅ Yes |
| `staging-apply` | `staging-plan` | `ocr-staging-cluster` | `staging-ocr-api` | ✅ Yes |
| `live-plan` | `staging-apply` | — | — | ✅ Yes |
| `live-apply` | `live-plan` | `ocr-live-cluster` | `live-ocr-api` | ❌ **Manual** (`trigger: false`) |

> **Live deploys are manually triggered.** The `live-apply` job requires a human to trigger it in Concourse after reviewing the `live-plan` output.

---

### API Tests (`cidev-api-tests`)

After a successful `cidev-apply`, a **Karate API test** suite is run against the deployed cidev service. Tests are sourced from the [`companieshouse/taf-api-karate`](https://github.com/companieshouse/taf-api-karate) repository and filtered by the tag `@ecs_ocr_api`. A test report is published to `REPORT_FOLDER: ecs-ocr-api-service-karate-cidev` regardless of pass/fail (`ensure`). Failures notify `#pa_build_failures`.

---

### Reporting Jobs (Weekly — every Thursday 22:00 UTC)

| Job | What it does | Failure channel |
| --- | --- | --- |
| `weekly-dependency-check` | Runs OWASP dependency CVE check against the `main` branch | `#pa_security_checks` |
| `weekly-sonar-analysis` | Runs a full SonarQube analysis against the `main` branch | `#pa_security_checks` |

---

### Utility Jobs

| Job | Group | Purpose |
| --- | --- | --- |
| `force-redeployment-cidev` | `dev-utilities` | Forces a new ECS deployment on `ocr-cidev-cluster / cidev-ocr-api` without a new release (e.g. to pick up updated secrets). 15-min readiness check included. |
| `force-redeployment-staging` | `fess-utilities` | Same for `ocr-staging-cluster / staging-ocr-api`. |
| `force-redeployment-live` | `fess-utilities` | Same for `ocr-live-cluster / live-ocr-api`. |

---

### Notifications

| Channel | Events |
| --- | --- |
| `#pa_build_failures` | PR analysis failures, build/test failures, cidev/staging Terraform failures |
| `#pa_security_checks` | Weekly dependency check failures, weekly Sonar failures |
| `#deployments` | Live Terraform plan/apply failures, live apply success |

---

### Versioning

Two independent semantic version sequences exist:

| Track | Tag format | Example | Governs |
| --- | --- | --- | --- |
| Application | `X.Y.Z` | `2.1.4` | JAR build, ECR image, ECS task version |
| Terraform | `ecs-service-X.Y.Z` | `ecs-service-1.3.0` | ECS service infrastructure |

Secrets and environment configuration are sourced from **HashiCorp Vault** during Terraform apply and written to **AWS SSM Parameter Store**, encrypted with the environment KMS key. The ECS task reads secrets directly from SSM at launch time.

---

# Runtime Environment

### AWS Infrastructure

| Component | Detail |
| --- | --- |
| Container platform | AWS ECS (Fargate) |
| Container registry | AWS ECR — `416670754337.dkr.ecr.eu-west-2.amazonaws.com/ocr-api` |
| Region | `eu-west-2` (London) |
| Load balancer | Application Load Balancer (`alb-{environment}-ocr-api`) |
| ALB listener | HTTPS port `443` |
| ALB path rule | `/ocr-api/*` (priority `24`) |
| Network | Application subnets within the `ocr-{environment}` VPC |
| Secret store | AWS SSM Parameter Store (KMS-encrypted) + HashiCorp Vault |
| Monitoring | CloudWatch alarms (enabled by default) |

### Per-environment ECS task sizing

| Environment | vCPU | Memory | Desired tasks | Min tasks | Max tasks | Scale down | Scale up |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `cidev` | 2 (2048) | 4 GB (4096) | 1 | 1 | 1 | 19:55 UTC daily | 06:05 UTC daily |
| `staging` | 2 (2048) | 6 GB (6144) | 1 | 1 | 1 | 19:55 UTC daily | 06:05 UTC daily |
| `live` | 4 (4096) | 8 GB (8192) | 2 | 2 | 3 | — | — |

`cidev` and `staging` scale down to zero tasks overnight via scheduled autoscaling to reduce costs. Live runs a minimum of 2 tasks at all times for high availability and can autoscale to 3 at 80% CPU utilisation.

### OS and runtime

| Component | Value |
| --- | --- |
| Base image | `416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21` (Amazon Corretto 21 on AL2023) |
| AL2023 minimum version | `2023.9.20251117` |
| JDK | Amazon Corretto 21 |
| Tesseract | Installed from AL2023 package repository via `dnf install tesseract tesseract-langpack-eng` |
| Tessdata path | `/usr/share/tesseract/tessdata` |
| `OMP_THREAD_LIMIT` | `1` |
| `LC_ALL` | `C` |

---

# Health Checks

### Application health check

**Endpoint:** `GET /ocr-api/healthcheck`
**Response:** `200 OK` with body `ocr-api is alive`

This endpoint also logs current thread pool statistics (queue depth, pool size, active threads) at `INFO` level on every call, providing a passive operational signal in the application logs.

### ECS / ALB health check configuration

The ALB target group health check and ECS task container health check both use:

| Setting | Value | Source |
| --- | --- | --- |
| Path | `/ocr-api/healthcheck` | `locals.tf:12` — `healthcheck_path` |
| Expected response code | `200` | `locals.tf:13` — `healthcheck_matcher` |
| Healthy threshold | 2 consecutive successes | `main.tf:46` — `healthcheck_healthy_threshold` |
| Grace period | 300 seconds | `main.tf:44` — `health_check_grace_period_seconds` |

A task that fails to respond `200` on `/ocr-api/healthcheck` after the grace period is marked unhealthy by the ALB and replaced by ECS.

### SonarQube code quality gate

Static analysis is run as part of the CI pipeline. The project is identified in SonarQube as `uk.gov.companieshouse:ocr-api`. A failed quality gate does not block deployment in the current configuration but is reported in the CI pipeline output.

---

# Differences: Local vs Production

| Aspect | Local (Docker / direct JAR) | Production (ECS Fargate) |
| --- | --- | --- |
| **Base image** | `eclipse-temurin:21.0.9_10-jre` (Debian-based) | `ci-corretto-runtime-21` (AL2023 / Amazon Corretto 21) |
| **Tesseract install** | `apt-get install tesseract-ocr` | `dnf install tesseract tesseract-langpack-eng` |
| **Tessdata** | Copied from `docker-resources/tessdata/2020-10-21/eng.traineddata` (October 2020 snapshot) | Installed with the OS package; current version from AL2023 repo |
| **Tessdata path** | `/usr/share/tessdata` | `/usr/share/tesseract/tessdata` |
| **Container entrypoint** | `sh -c "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"` | `./docker_start.sh` → `exec java -jar -Djava.security.egd=file:/dev/./urandom -Dserver.port="${PORT}" ocr-api.jar` |
| **JAR location in container** | `/app.jar` (copied from local build output) | `/opt/ocr-api.jar` (copied from CI build artifact) |
| **`JAVA_OPTS`** | Configurable via env var | Not used — fixed JVM flags only |
| **Port binding** | `docker run -p 8080:8080` (explicit mapping) | ECS exposes container port `8080`; ALB terminates TLS on `443` and forwards to `8080` |
| **TLS** | None (plain HTTP) | TLS terminated at the ALB; internal traffic is HTTP |
| **Secret management** | Environment variables set manually by the developer | Injected at ECS task launch from AWS SSM Parameter Store (sourced from Vault) |
| **Scaling** | Single instance | `cidev`/`staging`: 1 task (scales to 0 overnight); `live`: 2–3 tasks |
| **Container user** | `root` (no `USER` directive in dev `Dockerfile`) | Defined by the `ci-corretto-runtime-21` base image (verify with base image docs) |
| **Live reload** | Available via Tilt (`Tiltfile.dev`) + Jib class sync | Not applicable |
| **Log output** | stdout (console) | CloudWatch Logs via ECS log driver |
