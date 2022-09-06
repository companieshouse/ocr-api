package uk.gov.companieshouse.ocr.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import javax.annotation.PostConstruct;
import java.time.Duration;

@Configuration
public class SpringConfiguration {

    @Value("${ocr.queue.capacity}")
    private int ocrQueueCapacity;

    @Value("${rest.client.timeout.seconds}")
    private int restClientTimeoutSeconds;

    @Value("${low.confidence.to.log}")
    private int lowConfidenceToLog;

    @Value("${host.white.list.values}")
    private String hostWhiteList;

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

        LOG.info("The value of ${ocr.queue.capacity} is          :     " + ocrQueueCapacity);
        LOG.info("The value of ${low.confidence.to.log} is       :     " + lowConfidenceToLog);
        LOG.info("The value of ${rest.client.timeout.seconds} is :     " + restClientTimeoutSeconds);
        LOG.info("The value of ${host.white.list.values} is      :     " + hostWhiteList);

		LOG.info("-------------------- End displaying spring application.properties  ----------------------------------");
	}

    public int getOcrQueueCapacity() {
        return ocrQueueCapacity;
    }

    public int getLowConfidenceToLog() {
        return lowConfidenceToLog;
    }

    public String getHostWhiteList() {
      return hostWhiteList;
    }

}
