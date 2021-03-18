package uk.gov.companieshouse.ocr.api.statistics;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@Tag(TestType.UNIT)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = StatisticsController.class)
public class StatisticsControllerTest {

	private final static String TEST_UUID = "UUID_123";
	private final static int TEST_QUEUE_SIZE = 1;
	private final static int TEST_TESSERACT_POOL_SIZE = 3;

	@Autowired
	private MockMvc mockMvc;
	
	@Value("${api.endpoint}")
    private String apiEndpoint;

	@MockBean
    private StatisticsService mockStatisticsService;

	@Test
	public void shouldGetStatistics() throws Exception {

		StatisticsDTO testStatistics = new StatisticsDTO();
		testStatistics.setInstanceUuid(TEST_UUID);
		testStatistics.setQueueSize(TEST_QUEUE_SIZE);
		testStatistics.setTesseractThreadPoolSize(TEST_TESSERACT_POOL_SIZE);
		
		when(mockStatisticsService.create()).thenReturn(testStatistics);

		mockMvc.perform(MockMvcRequestBuilders.get(apiEndpoint + StatisticsController.STATS_PARTIAL_URL))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.instance_uuid", is(TEST_UUID)))
		.andExpect(jsonPath("$.queue_size", is(TEST_QUEUE_SIZE)))
		.andExpect(jsonPath("$.tesseract_thread_pool_size", is(TEST_TESSERACT_POOL_SIZE)));
	}
 }

