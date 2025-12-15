# Build:  docker build -t poc-ocr-tools .
# Run: docker run -t -i -p 8080:8080 poc-ocr-tools

FROM eclipse-temurin:21.0.9_10-jre

RUN apt-get -y update && apt-get -y upgrade

# Install tesseract library
RUN apt-get install tesseract-ocr -y && apt-get clean -y

# Download last language package
RUN mkdir -p /usr/share/tessdata

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
