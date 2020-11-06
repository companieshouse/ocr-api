package uk.gov.companieshouse.ocr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OcrApiApplication {
    
    public static final String APPLICATION_NAME_SPACE = "ocr-api";

    public static void main(String[] args) {
		SpringApplication.run(OcrApiApplication.class, args);
	}
}
