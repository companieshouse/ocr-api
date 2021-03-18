package uk.gov.companieshouse.ocr.api.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("${api.endpoint}")
@RestController
public class StatisticsController {

    public static final String STATS_PARTIAL_URL = "/statistics";
    
    @Autowired
    private StatisticsService statisticsService;

    @GetMapping(STATS_PARTIAL_URL)
    public @ResponseBody ResponseEntity<StatisticsDto> getStatistics() {

        return new ResponseEntity<>(statisticsService.generateStatistics(), HttpStatus.OK);
    }
    
}
