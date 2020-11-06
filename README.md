# ocr-api

A microservice to extract text from images. This uses Tess4J which itself is a small (Java Native Access) wrapper around Tesseract.

The initial drop of this microservice converts TIFF files to text.

## Requirements

- Java 11
- Maven
- Docker

## Usage

- Run `mvn package` to build JAR **(using Java 11)**
- Run `docker build -t ocr-api .` to build docker image
- Run `docker run -e HUMAN_LOG=1 -e LOGLEVEL=debug -t -i -p 8080:8080 ocr-api` to run image

## Testing Locally

### Using curl

- To call API for TIFF, POST `http://localhost:8080//api/ocr/image/extractText` passing in a file parameter as the tiff file to OCR and the "external_reference_id" field

Example:

``` bash
curl -F file=@"src/test/resources/sample-articles-of-association.tif" -F external_reference_id="curl test reference"  http://localhost:8080/api/ocr/image/extractText
```

### Using maven

Tests use jUnit5 tags and use the maven property "included.tests" to specify which ones to run

### Integration test

This allows you to locally test the application does an actual OCR image to text conversion

``` bash
mvn test -Dincluded.tests=integration-test
```
