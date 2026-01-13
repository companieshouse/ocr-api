ARG IMAGE_VERSION="latest"
FROM 416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21:${IMAGE_VERSION}

ARG AL2023_MIN_VER=2023.9.20251117

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN dnf --releasever="${AL2023_MIN_VER}" update -y && \
    dnf -y install \
        spal-release && \
    dnf install -y \
        tesseract && \
    dnf clean all

# Set the location of the jar
ENV MICROSERVICE_HOME=/usr/microservices

# Limit number of threads to 1
ENV OMP_THREAD_LIMIT=1

# Set the name of the jar
ENV APP_FILE=ocr-api.jar

ENV LC_ALL=C

WORKDIR /opt
COPY /app .
COPY docker_start.sh .
CMD ["./docker_start.sh"]
