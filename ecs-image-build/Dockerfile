ARG IMAGE_VERSION="latest"
FROM 416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21:${IMAGE_VERSION}

ARG AL2023_MIN_VER=2023.9.20251117

RUN dnf --releasever="${AL2023_MIN_VER}" update -y && \
    dnf install -y spal-release && \
    dnf install -y tesseract \
        tesseract-langpack-eng && \
    dnf clean all

# Limit number of threads to 1
ENV OMP_THREAD_LIMIT=1
ENV TESSDATA_PREFIX=/usr/share/tesseract/tessdata
ENV LC_ALL=C

WORKDIR /opt
COPY /app .
COPY docker_start.sh .
CMD ["./docker_start.sh"]
