package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.commons.lang.time.StopWatch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.companieshouse.ocr.api.SpringConfiguration;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {AsyncImageOcrApiController.class , SpringConfiguration.class})
@TestPropertySource(properties = {
    "ocr.queue.capacity=1",
    "low.confidence.to.log=40",
    "host.white.list=testurl.com"
})
class AsyncImageOcrApiControllerTest {

    @Value("${api.endpoint}")
    private String apiEndpoint;
    
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OcrRequestService ocrRequestService;

    @MockBean
    private MonitoringService monitoringService;

    @Test
    void shouldAcceptextractTextRequest() throws Exception {

        mockMvc.perform(post(apiEndpoint + AsyncImageOcrApiController.TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"app_id\":\"myapp\",\"response_id\": \"9613245852\", \"image_endpoint\": \"http://testurl.com/image?id=9613245852\",\"converted_text_endpoint\":\"http://testurl.com/ocr-results/\"}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldRejectExtractTextRequestWithNoBody() throws Exception {

        mockMvc.perform(post(apiEndpoint + AsyncImageOcrApiController.TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(SyncImageOcrApiController.NO_REQUEST_BODY_ERROR_MESSAGE)));
    }

    @Test
    void shouldRejectExtractTextRequestWithMissingParameter() throws Exception {

        mockMvc.perform(post(apiEndpoint + AsyncImageOcrApiController.TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"app_id\":\"myapp\",\"response_id\": \"9613245852\",\"converted_text_endpoint\":\"http://testurl.com/ocr-results/\"}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("invalid input[Missing required value image_endpoint]")));
    }

    @Test
    void shouldCatchOverload() throws Exception {

        doThrow(new TaskRejectedException("Test exception")).when(ocrRequestService)
                .handleAsynchronousRequest(any(OcrRequest.class), any(StopWatch.class));

        mockMvc.perform(post(apiEndpoint + AsyncImageOcrApiController.TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{ \"app_id\":\"myapp\",\"response_id\": \"9613245852\", \"image_endpoint\": \"http://testurl.com/image?id=9613245852\",\"converted_text_endpoint\":\"http://testurl.com/ocr-results/\"}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.context_id", is("myapp-9613245852")))
                .andExpect(jsonPath("$.error_message", is("Service Overloaded")));
    }

    @Test
    void shouldCatchUncaughtExceptionInController() throws Exception {

        doThrow(new RuntimeException("Test exception")).when(ocrRequestService)
                .handleAsynchronousRequest(any(OcrRequest.class), any(StopWatch.class));

        mockMvc.perform(post(apiEndpoint + AsyncImageOcrApiController.TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{ \"app_id\":\"myapp\",\"response_id\": \"9613245852\", \"image_endpoint\": \"http://testurl.com/image?id=9613245852\",\"converted_text_endpoint\":\"http://testurl.com/ocr-results/\"}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_message", is(AsyncImageOcrApiController.CONTROLLER_ERROR_MESSAGE)));
    }

}
