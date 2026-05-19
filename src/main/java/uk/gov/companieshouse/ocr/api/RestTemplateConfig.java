package uk.gov.companieshouse.ocr.api;

import java.time.Duration;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder, SpringConfiguration springConfiguration) {
        return  restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(springConfiguration.getRestClientTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(springConfiguration.getRestClientTimeoutSeconds()))
                .build();
    }

}