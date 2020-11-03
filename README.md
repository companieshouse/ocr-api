# ocr-api

Service to extract text from images. This uses Tess4J which itself is a small (Java Native Access) wrapper around Tesseract.

The initial drop of the service converts TIFF files to text.

## Requirements

- Java 11
- Maven
- Docker

## Usage

- Run `mvn package` to build JAR **(using Java 11)**
- Run `docker build -t poc-ocr-tools .` to build docker image
- Run `docker run -t -i -p 8080:8080 poc-ocr-tools` to run image (Tesseract only)