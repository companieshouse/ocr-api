package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.MOCK_TIFF_CONTENT;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.companieshouse.ocr.api.TestObjectMother;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
class OcrRequestServiceTest {

    @Mock
    private ImageRestClient imageRestClient;

    @Mock
    private ImageOcrService imageOcrService;

    @InjectMocks
    private OcrRequestService ocrRequestService;

    private final OcrRequest ocrRequest = TestObjectMother.getStandardOcrRequest();

/*
    @Test
    void successfulOcrRequest() throws OcrRequestException {

        // given
        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint())).thenReturn(MOCK_TIFF_CONTENT);
        when(imageOcrService.extractTextFromImageBytes(contextId, imageBytes, responseId, timeOnQueueStopWatch))

        // when
        ocrRequestService.handleRequest(ocrRequest);

        // verify when more methods are added to the service class
    }*/

    @Test
    void failGetImage() throws OcrRequestException {

        // when(imageOcrService.extractTextFromImage(eq(CONTEXT_ID), eq(file), eq(RESPONSE_ID), any(StopWatch.class)))
       // .thenThrow(new CompletionException("General", new IOException("IOException test")));

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint()))
            .thenThrow(new OcrRequestException("Mock failure", OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE, new IOException("IOException test")));

        // when
        ocrRequestService.handleRequest(ocrRequest); 

        // verify when more methods are added to the service class
    }
    
}
