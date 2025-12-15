# Build:  docker build -t poc-ocr-tools .
# Run: docker run -t -i -p 8080:8080 poc-ocr-tools

FROM eclipse-temurin:21.0.9_10-jre

RUN apt-get -y update && apt-get -y upgrade

# Install maven for testing
#RUN apt-get -y install maven

# Install tesseract library
RUN apt-get install tesseract-ocr -y && apt-get clean -y

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
ENV APP_FILE target/ocr-api-unversioned.jar

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


