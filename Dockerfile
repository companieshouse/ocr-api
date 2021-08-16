# Creates the base image for jib to build the final production image from
# Build:  docker build -t tesseract-base-image .

FROM openjdk:11-jdk

ENV MICROSERVICE_HOME=/usr/microservices \
    OMP_THREAD_LIMIT=1 \
    LC_ALL=C

RUN apt-get update \
  && apt-get -y upgrade \
  && apt-get install -y tesseract-ocr --no-install-recommends \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir -p /usr/share/tessdata

