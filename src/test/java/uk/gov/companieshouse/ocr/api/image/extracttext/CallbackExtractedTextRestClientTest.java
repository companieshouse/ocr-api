package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.CONTEXT_ID;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.RESPONSE_ID;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.EXTRACTED_TEXT_ENDPOINT;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.TIME_PROCESSING;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.gov.companieshouse.ocr.api.TestObjectMother;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
public class CallbackExtractedTextRestClientTest {

    private ExtractTextResultDto extractTextResultDto;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CallbackExtractedTextRestClient callbackExtractedTextRestClient;
    
    @BeforeEach
    void setupTests() {
        extractTextResultDto = TestObjectMother.getStandardExtractTextResultDto();
    }

    @Test
    void testSendExtractedTextSuccessfully() throws OcrRequestException {

        // given
        when(restTemplate.postForEntity(eq(EXTRACTED_TEXT_ENDPOINT), any(), any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        callbackExtractedTextRestClient.sendTextResult(EXTRACTED_TEXT_ENDPOINT, extractTextResultDto);

        // then
        verify(restTemplate).postForEntity(eq(EXTRACTED_TEXT_ENDPOINT), any(), any());
    }

    @Test
    void testSendExtractedTextUnsuccessful() {
        // given
        when(restTemplate.postForEntity(eq(EXTRACTED_TEXT_ENDPOINT), any(), any()))
                .thenThrow(RestClientException.class);

        var ocrRequestAssertion = assertThrows(OcrRequestException.class, () ->
            callbackExtractedTextRestClient.sendTextResult(EXTRACTED_TEXT_ENDPOINT, extractTextResultDto));

        assertEquals(999, ocrRequestAssertion.getResultCode().getCode());
    }


    @Test
    void testSendErrorResultSuccessfully()  {

        // given
        when(restTemplate.postForEntity(eq(EXTRACTED_TEXT_ENDPOINT), any(), any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        callbackExtractedTextRestClient.sendTextResultError(CONTEXT_ID, RESPONSE_ID, EXTRACTED_TEXT_ENDPOINT, OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT, TIME_PROCESSING);

        // then
        verify(restTemplate).postForEntity(eq(EXTRACTED_TEXT_ENDPOINT), any(), any());
    }


    @Test
    void testSendErrorResultUnSuccessfullyExceptionTrapped()  {

        // given
        when(restTemplate.postForEntity(eq(EXTRACTED_TEXT_ENDPOINT), any(), any()))
                .thenThrow(RestClientException.class);

        // when
        callbackExtractedTextRestClient.sendTextResultError(CONTEXT_ID, RESPONSE_ID, EXTRACTED_TEXT_ENDPOINT, OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT, TIME_PROCESSING);

        // then
        verify(restTemplate).postForEntity(eq(EXTRACTED_TEXT_ENDPOINT), any(), any());
    }
    
}
