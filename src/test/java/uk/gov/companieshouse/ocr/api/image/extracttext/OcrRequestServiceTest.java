package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.MOCK_TIFF_CONTENT;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang.time.StopWatch;
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


    @Test
    void successfulOcrRequest() throws OcrRequestException, IOException {

        // given
        var textConversionResult = TestObjectMother.getStandardTextConversionResult();
        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint())).thenReturn(MOCK_TIFF_CONTENT);
        when(imageOcrService.extractTextFromImageBytes(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class)))
           .thenReturn(CompletableFuture.completedFuture(textConversionResult));

        // when
        var ocrRequestStopWatch = new StopWatch();
        ocrRequestStopWatch.start();
        ocrRequestService.handleRequest(ocrRequest, ocrRequestStopWatch);

        verify(imageRestClient).getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint());
        verify(imageOcrService).extractTextFromImageBytes(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class));
    }

    @Test
    void failGetImage() throws OcrRequestException, IOException {

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint()))
            .thenThrow(new OcrRequestException("Mock failure", OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE, new IOException("IOException test")));

        // when
        ocrRequestService.handleRequest(ocrRequest, new StopWatch()); 

        // verify when more methods are added to the service class
        verify(imageOcrService, never()).extractTextFromImageBytes(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class));
    }
    
}
