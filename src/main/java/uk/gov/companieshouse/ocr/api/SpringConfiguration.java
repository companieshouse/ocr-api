package uk.gov.companieshouse.ocr.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Configuration
public class SpringConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Value("${ocr.queue.capacity}")
    private int ocrQueueCapacity;

    @Value("${rest.client.timeout.seconds}")
    private int restClientTimeoutSeconds;

    @Value("${low.confidence.to.log}")
    private int lowConfidenceToLog;

    @Value("${host.white.list}")
    private String hostWhiteList;

    @Value("${tessdata.path}")
    private String tessdataPath;

    @PostConstruct
	private void displaySpringProperties() {
		LOG.info("-------------------- Displaying spring application.properties  ----------------------------------");

        LOG.info("The value of ${ocr.queue.capacity} is          :     " + ocrQueueCapacity);
        LOG.info("The value of ${low.confidence.to.log} is       :     " + lowConfidenceToLog);
        LOG.info("The value of ${rest.client.timeout.seconds} is :     " + restClientTimeoutSeconds);
        LOG.info("The value of ${host.white.list.values} is      :     " + hostWhiteList);
        LOG.info("The value of ${tessdata.path} is               :     " + tessdataPath);

		LOG.info("-------------------- End displaying spring application.properties  ----------------------------------");
	}

    public int getOcrQueueCapacity() {
        return ocrQueueCapacity;
    }

    public int getRestClientTimeoutSeconds() {
        return restClientTimeoutSeconds;
    }

    public int getLowConfidenceToLog() {
        return lowConfidenceToLog;
    }

    public String getHostWhiteList() {
      return hostWhiteList;
    }

    public String getTessdataPath() {
        return tessdataPath;
    }
}
