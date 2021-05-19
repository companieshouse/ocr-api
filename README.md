# ocr-api

A microservice to extract text from images. This uses Tess4J which itself is a small (Java Native Access) wrapper around Tesseract. As well as returning the extracted text some metadata relating to this service is also returned [data returned](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ExtractTextResultDto.java). Optionally a "context id" can be sent for use in context logging. If it is not sent then the "request id" is used for context logging.

The initial drop of this microservice converts TIFF files to text.

## Requirements

- Java 11
- Maven
- Docker

## Usage

Set the environmental variables OCR_TESSERACT_THREAD_POOL_SIZE, HUMAN_LOG and LOGLEVEL

- Run `make dev` to build JAR (versioned in target and unversioned in top level d) and run the unit tests **(using Java 11)**
- Run `docker build -t ocr-api .` to build the docker image
- Run `docker run -e OCR_TESSERACT_THREAD_POOL_SIZE -e HUMAN_LOG -e LOGLEVEL -t -i -p 8080:8080 ocr-api` to run the docker image

## Tesseract Training data

This is used by the Tesseract engine to help in the text recognition. We store the currently used data within configuration management for consistency and speed of the docker build.

To Update the training data, download the `eng.traineddata` file from one of the following URL (note that using the "best" data slows down the time of the OCR conversion and has not yet be shown to significantly make it better):

- [Standard data](https://github.com/tesseract-ocr/tessdata)
- [Best data](https://github.com/tesseract-ocr/tessdata_best)

Store the data file in `docker-resources/tessdata/` with a timestamp and adjust the Docker file to use it.

## Metadata

A set of metadata related to the OCR process is created and logged in the application with a subset of it returned as part of the API. There are two types of meta data:

- Confidence data obtained from the Tesseract API,
- Timings (time of the internal queue between the controller and asynchronous service class it calls, the ocr processing time and the total time within the application)

Internally this data is logged while externally a subset of it is returned in the API.

See:

- [internal class](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/TextConversionResult.java)
- [external class](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ExtractTextResultDTO.java)
- [mapper class](src/main/java/uk/gov/companieshouse/ocr/api/image/extracttext/ImageOcrTransformer.java)

## Environment Variables

The following is a list of application specific environment variables for the service to run:

Name                                        | Description                                                               | Example Value
------------------------------------------- | ------------------------------------------------------------------------- | ------------------------
OCR_TESSERACT_THREAD_POOL_SIZE              | Number of threads to run the Tesseract Conversion process                 | 4  (default value)

## The stats end point

Name                       | Description
---------------------------| ------------------------------------------------------------
queue_size                 | The number of items on the internal queue waiting to be processed by the Tesseract threads
instance_uuid              | UUID for when multiple instance of ocr-api are running in the same AWS ECS Cluster (or instance restarts)
tesseract_thread_pool_size | The number of Java threads used in the Tesseract image to text conversion (static)  

## Testing Deployment

### Using curl

- To call API for TIFF, POST `http://localhost:8080/ocr-apr/api/ocr/image/tiff/extractText` passing in a file parameter as the tiff file to OCR and the "responseId" field (optionally add a "contextId" where you want context logging to be more than the "responseId")

Example:

``` bash
# With Context ID
curl -F file=@"src/test/resources/sample-articles-of-association.tif" -F responseId="curl test response id" -F contextId="CHIPS-AA-34568493" http://localhost:8080/ocr-api/api/ocr/image/tiff/extractText

# Without Context ID
curl -F file=@"src/test/resources/sample-articles-of-association.tif" -F responseId="curl test response id"  http://localhost:8080/ocr-api/api/ocr/image/tiff/extractText
```

For heath check:

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
