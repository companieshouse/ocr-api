# `ocr-api`

A microservice to extract text from images. This uses Tess4J which itself is a small (Java Native Access) wrapper around Tesseract. As well as returning the extracted text some metadata relating to this service is also returned [data returned](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ExtractTextResultDto.java).

The `ocr-api` has one thread pool (with a blocking queue) that protects the system from being overloaded (implemented by a ThreadPoolTaskExecutor). In the normal running of this microservice this queue should have very few entries on it.

The initial drop of this microservice converts TIFF files to text.

## Call Types

### Asynchronous

Endpoint = `[server address]/ocr-api/api/ocr/image/tiff/extractTextRequest`

The request to the controller is first vetted and then handed off to an asynchronous thread and a 202 Http status is returned to the client. On the asynchronous thread the following steps are made:

- Get the image provided in the OCR request,
- Convert the image to text,
- Send the results back via a callback URL provided in the OCR Request.

### Synchronous

Endpoint = `[server address]/ocr-api/api/ocr/image/tiff/extractText`

The file to be converted is uploaded to the controller and the results (extracted text plus meta data) are returned.

## Logging

This project uses the Companies House Structured logging framework for writing logging messages. A set of log messages are written to allow this microservice to be better monitored. 

These key log messages contain a map of values that can be used by systems such as CloudWatch for log queries and Dashboards. They use constants in the `JsonContants.java` file to keep the map key values consistent for all log messages. Since the key values are used in the [ocr-api-stack](https://github.com/companieshouse/ocr-api-stack) project then they must be updated in both projects together.

## Requirements

- Java 11
- Maven
- Docker

## Usage

Set the environmental variables `OCR_TESSERACT_THREAD_POOL_SIZE`, `OCR_QUEUE_CAPACITY`, `HUMAN_LOG` and `LOGLEVEL`

- Run `make dev` to build JAR (versioned in target and unversioned in top level d) and run the unit tests **(using Java 11)**
- Run `docker build -t ocr-api .` to build the docker image
- Run `docker run -e OCR_TESSERACT_THREAD_POOL_SIZE -e OCR_QUEUE_CAPACITY -e HUMAN_LOG -e LOGLEVEL -t -i -p 8080:8080 ocr-api` to run the docker image

## Tesseract Training data

This is used by the Tesseract engine to help in the text recognition. We store the currently used data within configuration management for consistency and speed of the docker build.

To Update the training data, download the `eng.traineddata` file from one of the following URL (note that using the "best" data slows down the time of the OCR conversion and has not yet be shown to significantly make it better):

- [Standard data](https://github.com/tesseract-ocr/tessdata)
- [Best data](https://github.com/tesseract-ocr/tessdata_best)

Store the data file in `docker-resources/tessdata/` with a timestamp and adjust the Docker file to use it.

## Metadata

A set of metadata related to the OCR process is created and logged in the application with a subset of it returned as part of the API. There are two types of meta data:

- Confidence data obtained from the Tesseract API,
- Timings (time of the internal queue between the controller and asynchronous service class it calls, the OCR processing time and the total time within the application)

Internally this data is logged while externally a subset of it is returned in the API.

See:

- [internal class](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/TextConversionResult.java)
- [external class](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ExtractTextResultDTO.java)
- [mapper class](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ImageOcrTransformer.java)

## Environment Variables

The following is a list of application specific environment variables for the service to run:

Name                                        | Description                                                               | Example Value
------------------------------------------- | ------------------------------------------------------------------------- | ------------------------
LOW_CONFIDENCE_TO_LOG                       | The minimum confidence value used for logging low confidence scores (logs lines with lower scores than the value set) | 40
OCR_TESSERACT_THREAD_POOL_SIZE              | Number of threads to run the Tesseract Conversion process                                                             | 4  (default value)
OCR_QUEUE_CAPACITY                          | Maximum number of OCR Requests in the OCR Queue before a 503 is returned                                              | 5

## The stats end point

Name                       | Description
---------------------------| ------------------------------------------------------------
instance_uuid              | UUID for when multiple instance of `ocr-api` are running in the same AWS ECS Cluster (or instance restarts)
queue_size                 | The number of items on the internal queue waiting to be processed by the Tesseract threads
active_pool_size           | the largest size of the pool since it was created.
pool_size                  | count of threads in the thread pool,
largest_pool_size          | count of threads in the thread pool currently running tasks.

## Testing Deployment

### Using curl

- To call API for TIFF, POST `http://localhost:8080/ocr-apr/api/ocr/image/tiff/extractText` passing in a file parameter as the tiff file to OCR and the "responseId" field (optionally add a "contextId" where you want context logging to be more than the "responseId")

Example:

``` bash
# With Context ID
curl -F file=@"src/test/resources/sample-articles-of-association.tif" -F responseId="curl test response id" -F contextId="SAMPLE_ARTICLES" http://localhost:8080/ocr-api/api/ocr/image/tiff/extractText
curl -F file=@"src/test/resources/blank-articles.tif" -F responseId="curl test response id" -F contextId="BLANK-TIFF" http://localhost:8080/ocr-api/api/ocr/image/tiff/extractText
curl -F file=@"src/test/resources/empty-articles.tif" -F responseId="curl test response id" -F contextId="EMPTY-TIFF" http://localhost:8080/ocr-api/api/ocr/image/tiff/extractText
curl -w '%{http_code}' -F file=@"src/test/resources/small-articles.tif" -F responseId="curl test response id" -F contextId="SMALL-TIFF" http://localhost:8080/ocr-api/api/ocr/image/tiff/extractText

# Without Context ID
curl -F file=@"src/test/resources/sample-articles-of-association.tif" -F responseId="curl test response id"  http://localhost:8080/ocr-api/api/ocr/image/tiff/extractText
```

For Asynchronous Endpoint

``` bash
curl -w '%{http_code}' --header "Content-Type: application/json" \
  --request POST \
  --data '{"app_id": "curl-test","image_endpoint": "http://testurl.com/cff/servlet/viewArticles?transaction_id=9613245852", "converted_text_endpoint": "http://testurl.com/ocr-results/", "response_id": "9613245852"}' \
  http://localhost:8080/ocr-api/api/ocr/image/tiff/extractTextRequest
```

For health check:

``` bash
curl -w '%{http_code}' http://localhost:8080/ocr-api/healthcheck
```

For statistics endpoint:

``` bash
curl -w '%{http_code}' http://localhost:8080/ocr-api/statistics
```

### Using maven

Tests use jUnit5 tags and use the maven property "included.tests" to specify which ones to run

### Integration test

This allows you to locally test the application does an actual OCR image to text conversion

``` bash
mvn test -Dincluded.tests=integration-test
```
