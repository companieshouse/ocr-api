package uk.gov.companieshouse.ocr.api.heathcheck;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    public static final String HEATH_CHECK_PARTIAL_URL = "/healthcheck";
    public static final String HEATH_CHECK_MESSAGE = "ALIVE";


    @GetMapping(HEATH_CHECK_PARTIAL_URL)
    public @ResponseBody ResponseEntity<String> heathCheck() {
        return new ResponseEntity<>(HEATH_CHECK_MESSAGE, HttpStatus.OK);

    }

} 
