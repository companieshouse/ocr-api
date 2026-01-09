ARG IMAGE_VERSION="latest"

# Build stage
FROM 416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21:${IMAGE_VERSION} as build

RUN yum -y update && \
    yum -y upgrade && \
    yum install clang -y && \
    yum install libpng-devel libtiff-devel zlib-devel libwebp-devel libjpeg-turbo-devel wget tar gzip -y && \
    wget https://github.com/DanBloomberg/leptonica/releases/download/1.75.1/leptonica-1.75.1.tar.gz && \
    tar -zxvf leptonica-1.75.1.tar.gz && \
    cd leptonica-1.75.1 && \
    ./configure && \
    make && \
    make install

RUN cd ~ && \
    yum install git-core libtool pkgconfig -y && \
    git clone --depth 1  https://github.com/tesseract-ocr/tesseract.git tesseract-ocr && \
    cd tesseract-ocr && \
    export PKG_CONFIG_PATH=/usr/local/lib/pkgconfig && \
    ./autogen.sh && \
    ./configure && \
    make && \
    make install

RUN mkdir -p /usr/local/share/tessdata && cd /usr/local/share/tessdata && \
    wget https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata && \
    wget https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata && \
    wget https://github.com/tesseract-ocr/tessdata/raw/main/hin.traineddata

ENV TESSDATA_PREFIX=/usr/local/share/tessdata

# Final runtime image
FROM 416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21:${IMAGE_VERSION}

RUN yum -y update && \
    yum -y upgrade && \
    yum install clang -y && \
    yum install libpng libtiff zlib libwebp libjpeg-turbo -y && \
    yum clean all

COPY --from=build /usr/local /usr/local

# Check the installation status
RUN tesseract --list-langs
RUN tesseract -v

# Set the location of the jar
ENV MICROSERVICE_HOME /usr/microservices

# Limit number of threads to 1
ENV OMP_THREAD_LIMIT 1

# Set the name of the jar
ENV APP_FILE ocr-api.jar

ENV LC_ALL C

WORKDIR /opt
COPY /app .
COPY docker_start.sh .
CMD ["./docker_start.sh"]
