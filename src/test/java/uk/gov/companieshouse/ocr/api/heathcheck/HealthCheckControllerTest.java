package uk.gov.companieshouse.ocr.api.heathcheck;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import uk.gov.companieshouse.ocr.api.groups.TestType;
import uk.gov.companieshouse.ocr.api.statistics.StatisticsDto;
import uk.gov.companieshouse.ocr.api.statistics.StatisticsService;

@Tag(TestType.UNIT)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = HealthCheckController.class)
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService mockStatisticsService; // used in logging
    
    @Value("${api.endpoint}")
    private String apiEndpoint;

    @Test
    void validateIsHealthy() throws Exception {

        StatisticsDto testStatistics = new StatisticsDto();
        testStatistics.setInstanceUuid("test-uuid");
        testStatistics.setTesseractQueueSize(2);
        testStatistics.setTesseractPoolSize(4);

        when(mockStatisticsService.generateStatistics()).thenReturn(testStatistics);

        mockMvc.perform(MockMvcRequestBuilders.get(apiEndpoint + HealthCheckController.HEALTH_CHECK_PARTIAL_URL))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString(HealthCheckController.HEALTH_CHECK_MESSAGE)));
    }
}