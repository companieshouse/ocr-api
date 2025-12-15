# Build:  docker build -t poc-ocr-tools .
# Run: docker run -t -i -p 8080:8080 poc-ocr-tools

ARG IMAGE_VERSION="latest"
FROM 416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21:${IMAGE_VERSION}

# Update base OS
RUN yum update -y

# Enable EPEL repository (required for tesseract on Amazon Linux)
RUN yum install -y epel-release

# Install maven for testing
#RUN yum install -y maven

# Install tesseract OCR
RUN yum install -y \
    tesseract \
    tesseract-langpack-eng \
    && yum clean all

# Download last language package
RUN mkdir -p /usr/share/tessdata
COPY docker-resources/tessdata/2020-10-21/eng.traineddata /usr/share/tessdata/eng.traineddata
#ADD https://github.com/tesseract-ocr/tessdata/raw/master/eng.traineddata /usr/share/tessdata/eng.traineddata
#ADD https://github.com/tesseract-ocr/tessdata_best/raw/master/eng.traineddata /usr/share/tessdata/eng.traineddata


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

# Open the port
EXPOSE 8080

# Copy script to configure location to download dependencies (to enable caching)
COPY docker-resources/configure-maven /usr/local/bin/configure-maven

# Copy our JAR
COPY $APP_FILE /app.jar

# Launch the Spring Boot application
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]


