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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ImageOcrApiController.class)
public class ImageOcrApiControllerTest {

    private final static String FILE_TEXT = "I am a tiff image of articles of association";
    private final static String RESPONSE_ID = "test-response-id";

    @Autowired
    private MockMvc mockMvc;
  
    @MockBean
    private ImageOcrService imageOcrService;

    @Mock
    private TextConversionResult mockResults;

    private MockMultipartFile file ;

    @BeforeEach
    private void setUpTests() throws Exception {
         
        file = new MockMultipartFile(ImageOcrApiController.FILE_REQUEST_PARAMETER_NAME, "hello.txt",
        "application/octet-stream", FILE_TEXT.getBytes());
    }

    @Test
    public void shouldextractTextFromTiff() throws Exception {

        when(mockResults.getExtractedText()).thenReturn(FILE_TEXT);
        when(mockResults.getDocumentAverageConfidence()).thenReturn(90f);
        when(mockResults.getDocumentMinimumConfidence()).thenReturn(80f);
        when(mockResults.getResponseId()).thenReturn(RESPONSE_ID);
        when(mockResults.getExtractTextProcessingTime()).thenReturn(3200l);

        when(imageOcrService.extractTextFromImage(eq(file), eq(RESPONSE_ID), any(StopWatch.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResults));

        mockMvc.perform(multipart(ImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(ImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.extracted_text", is(FILE_TEXT)))
                .andExpect(jsonPath("$.response_id", is(RESPONSE_ID)))
                .andExpect(jsonPath("$.ocr_processing_time_ms", is(3200)))
                .andExpect(jsonPath("$.total_processing_time_ms", org.hamcrest.Matchers.any(Integer.class)))
                .andExpect(jsonPath("$.average_confidence_score", is(90)))
                .andExpect(jsonPath("$.lowest_confidence_score", is(80)));
    }

    @Test
    public void shouldCatchUncaughtExceptionInController() throws Exception {

        mockMvc.perform(multipart(ImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(ImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_message", is(ImageOcrApiController.CONTROLLER_ERROR_MESSAGE)))
                .andExpect(jsonPath("$.response_id").doesNotExist());
    }

    @Test
    public void shouldCatchFutureExceptionInController() throws Exception {

        when(imageOcrService.extractTextFromImage(eq(file), eq(RESPONSE_ID), any(StopWatch.class)))
        .thenThrow(new CompletionException("General", new IOException("IOException test")));

        mockMvc.perform(multipart(ImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(ImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_message", is(ImageOcrApiController.GENERAL_SERVICE_ERROR_MESSAGE)))
                .andExpect(jsonPath("$.response_id").doesNotExist());
    }

    @Test
    public void shouldCatchFutureExceptionWithApplicationErrorInController() throws Exception {

        when(imageOcrService.extractTextFromImage(eq(file), eq(RESPONSE_ID), any(StopWatch.class)))
        .thenThrow(new CompletionException("General", new TextConversionException(RESPONSE_ID, new IOException("Wrapped IOException test"))));

        mockMvc.perform(multipart(ImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL).file(file)
                .param(ImageOcrApiController.RESPONSE_ID_REQUEST_PARAMETER_NAME, RESPONSE_ID)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_message", is(ImageOcrApiController.TEXT_CONVERSION_ERROR_MESSAGE)))
                .andExpect(jsonPath("$.response_id", is(RESPONSE_ID)));
    }
}
