package uk.gov.companieshouse.ocr.api;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Configuration
public class SpringConfiguration {

    @Value("${rest.client.timeout.seconds}")
    private int restClientTimeoutSeconds;

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Bean
    RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return  restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(restClientTimeoutSeconds))
            .setReadTimeout(Duration.ofSeconds(restClientTimeoutSeconds))
            .build();
    }

    @PostConstruct
	private void displaySpringProperties() {
		LOG.info("-------------------- Displaying spring application.properties  ----------------------------------");

		LOG.info("The value of ${rest.client.timeout.seconds} is :     " + restClientTimeoutSeconds);

		LOG.info("-------------------- End displaying spring application.properties  ----------------------------------");
	}
    
}
