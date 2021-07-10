package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang.time.StopWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = SyncImageOcrApiController.class)
class SyncImageOcrApiControllerTest {

    private static final String CONTEXT_ID = "test-context-id";
    private static final String FILE_TEXT = "I am a tiff image of articles of association";
    private static final String RESPONSE_ID = "test-response-id";

    @Value("${api.endpoint}")
    private String apiEndpoint;
    
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OcrRequestService ocrRequestService;

    @MockBean
    private MonitoringService monitoringService;

    @Mock
    private TextConversionResult mockResults;

    private MockMultipartFile file ;

    @BeforeEach
    private void setUpTests() throws Exception {
         
        file = new MockMultipartFile(SyncImageOcrApiController.FILE_REQUEST_PARAMETER_NAME, "hello.txt",
        "application/octet-stream", FILE_TEXT.getBytes());
    }

    @Test
    void shouldextractTextFromTiffNoContextId() throws Exception {

        when(mockResults.getExtractedText()).thenReturn(FILE_TEXT);
        when(mockResults.getDocumentAverageConfidence()).thenReturn(90f);
        when(mockResults.getDocumentMinimumConfidence()).thenReturn(80f);
        when(mockResults.getResponseId()).thenReturn(RESPONSE_ID);
        when(mockResults.getExtractTextProcessingTime()).thenReturn(3200l);

        when(ocrRequestService.handleSynchronousRequest(eq(RESPONSE_ID), eq(file.getBytes()), eq(RESPONSE_ID), any(StopWatch.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResults));

        mockMvc.perform(multipart(apiEndpoint + SyncImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(SyncImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.extracted_text", is(FILE_TEXT)))
                .andExpect(jsonPath("$.response_id", is(RESPONSE_ID)))
                .andExpect(jsonPath("$.ocr_processing_time_ms", is(3200)))
                .andExpect(jsonPath("$.total_processing_time_ms", org.hamcrest.Matchers.any(Integer.class)))
                .andExpect(jsonPath("$.average_confidence_score", is(90)))
                .andExpect(jsonPath("$.lowest_confidence_score", is(80)))
                .andExpect(jsonPath("$.result_code", is(0)));
    }

    @Test
    void shouldextractTextFromTiffWithContextId() throws Exception {

        when(mockResults.getExtractedText()).thenReturn(FILE_TEXT);
        when(mockResults.getDocumentAverageConfidence()).thenReturn(90f);
        when(mockResults.getDocumentMinimumConfidence()).thenReturn(80f);
        when(mockResults.getResponseId()).thenReturn(RESPONSE_ID);
        when(mockResults.getExtractTextProcessingTime()).thenReturn(3200l);

        when(ocrRequestService.handleSynchronousRequest(eq(CONTEXT_ID), eq(file.getBytes()), eq(RESPONSE_ID), any(StopWatch.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResults));

        mockMvc.perform(multipart(apiEndpoint + SyncImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(SyncImageOcrApiController.CONTEXT_ID_REQUEST_PARAMETER_NAME, CONTEXT_ID)
                .param(SyncImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.extracted_text", is(FILE_TEXT)))
                .andExpect(jsonPath("$.response_id", is(RESPONSE_ID)))
                .andExpect(jsonPath("$.ocr_processing_time_ms", is(3200)))
                .andExpect(jsonPath("$.total_processing_time_ms", org.hamcrest.Matchers.any(Integer.class)))
                .andExpect(jsonPath("$.average_confidence_score", is(90)))
                .andExpect(jsonPath("$.lowest_confidence_score", is(80)))
                .andExpect(jsonPath("$.result_code", is(0)));
    }

    @Test
    void shouldCatchUncaughtExceptionInController() throws Exception {

        mockMvc.perform(multipart(apiEndpoint + SyncImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(SyncImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_message", is(SyncImageOcrApiController.CONTROLLER_ERROR_MESSAGE)))
                .andExpect(jsonPath("$.response_id").doesNotExist());
    }

    @Test
    void shouldCatchFutureExceptionInController() throws Exception {

        when(ocrRequestService.handleSynchronousRequest(eq(CONTEXT_ID), eq(file.getBytes()), eq(RESPONSE_ID), any(StopWatch.class)))
        .thenThrow(new CompletionException("General", new IOException("IOException test")));

        mockMvc.perform(multipart(apiEndpoint + SyncImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(SyncImageOcrApiController.CONTEXT_ID_REQUEST_PARAMETER_NAME, CONTEXT_ID)
                .param(SyncImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_message", is(SyncImageOcrApiController.GENERAL_SERVICE_ERROR_MESSAGE)))
                .andExpect(jsonPath("$.response_id").doesNotExist());
    }

    @Test
    void shouldCatchFutureExceptionWithApplicationErrorInController() throws Exception {

        when(ocrRequestService.handleSynchronousRequest(eq(CONTEXT_ID), eq(file.getBytes()), eq(RESPONSE_ID), any(StopWatch.class)))
        .thenThrow(new CompletionException("General", new TextConversionException(CONTEXT_ID, RESPONSE_ID, new IOException("Wrapped IOException test"))));

        mockMvc.perform(multipart(apiEndpoint + SyncImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(SyncImageOcrApiController.CONTEXT_ID_REQUEST_PARAMETER_NAME, CONTEXT_ID)
                .param(SyncImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_message", is(SyncImageOcrApiController.TEXT_CONVERSION_ERROR_MESSAGE)))
                .andExpect(jsonPath("$.response_id", is(RESPONSE_ID)));
    }
}
