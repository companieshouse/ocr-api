ARG IMAGE_VERSION="latest"
FROM 416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21:${IMAGE_VERSION}

# Update base OS
RUN yum update -y

# Enable EPEL repository (required for tesseract on Amazon Linux)
RUN yum install -y epel-release

# Install tesseract OCR
RUN yum install -y \
    tesseract \
    tesseract-langpack-eng \
    && yum clean all \

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
