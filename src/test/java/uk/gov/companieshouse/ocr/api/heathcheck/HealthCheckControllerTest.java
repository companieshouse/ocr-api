package uk.gov.companieshouse.ocr.api.heathcheck;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = HealthCheckController.class)
public class HealthCheckControllerTest {

    @Autowired
	private MockMvc mockMvc;
	
	@Value("${api.endpoint}")
    private String apiEndpoint;

	@Test
	void validateIsHealthy() throws Exception {

           mockMvc.perform(MockMvcRequestBuilders.get(apiEndpoint + HealthCheckController.HEALTH_CHECK_PARTIAL_URL))
		   .andExpect(status().isOk())
		   .andExpect(content().string(containsString(HealthCheckController.HEALTH_CHECK_MESSAGE)));
	}
}