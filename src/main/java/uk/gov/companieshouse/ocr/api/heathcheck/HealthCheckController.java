package uk.gov.companieshouse.ocr.api.heathcheck;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.statistics.StatisticsService;

@RequestMapping("${api.endpoint}")
@RestController
public class HealthCheckController {

    public static final String HEALTH_CHECK_PARTIAL_URL = "/healthcheck";
    public static final String HEALTH_CHECK_MESSAGE = "ocr-api is alive";

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping(HEALTH_CHECK_PARTIAL_URL)
    public @ResponseBody ResponseEntity<String> heathCheck() {

        LOG.info("Health Statistics", statisticsService.generateStatistics().toMap());

        return new ResponseEntity<>(HEALTH_CHECK_MESSAGE, HttpStatus.OK);
    }

} 
