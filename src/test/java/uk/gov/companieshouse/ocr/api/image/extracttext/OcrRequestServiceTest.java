package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Mock
    private CallbackExtractedTextRestClient callbackExtractedTextRestClient;

    @Mock
    private MonitoringService monitoringService;

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
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch());

        // then send successful results
        verify(callbackExtractedTextRestClient).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(monitoringService).logSuccess(eq(ocrRequest.getContextId()), any(MonitoringFields.class));
        verify(callbackExtractedTextRestClient, never()).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), any(OcrRequestException.ResultCode.class), anyLong());
    }

    private StopWatch startedOcrRequestStopWatch() {
        var ocrRequestStopWatch = new StopWatch();
        ocrRequestStopWatch.start();
        return ocrRequestStopWatch;
    }

    @Test
    void failGetImage() throws OcrRequestException, IOException {

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint()))
            .thenThrow(new OcrRequestException("Mock failure", OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE, new IOException("IOException test")));

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(imageOcrService, never()).extractTextFromImageBytes(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class));
        verify(callbackExtractedTextRestClient, never()).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE), anyLong());
    }

    @Test
    void failImageToText() throws OcrRequestException, IOException {

        CompletableFuture<TextConversionResult> futureExpection = new CompletableFuture<>();
        futureExpection.completeExceptionally(new IOException("Image to text failure test"));

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint())).thenReturn(MOCK_TIFF_CONTENT);
        when(imageOcrService.extractTextFromImageBytes(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class)))
           .thenReturn(futureExpection);

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(callbackExtractedTextRestClient, never()).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT), anyLong());
    }


    @Test
    void failSendingResults() throws OcrRequestException, IOException {

        CompletableFuture<TextConversionResult> futureExpection = new CompletableFuture<>();
        futureExpection.completeExceptionally(new IOException("Image to text failure test"));

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint())).thenReturn(MOCK_TIFF_CONTENT);
        when(imageOcrService.extractTextFromImageBytes(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class)))
           .thenReturn(futureExpection);

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(callbackExtractedTextRestClient, never()).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT), anyLong());
    }


    @Test
    void unexpectedException() throws OcrRequestException, IOException {

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint()))
            .thenThrow(new RuntimeException("Unexpected runtime exception"));

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(imageOcrService, never()).extractTextFromImageBytes(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class));
        verify(callbackExtractedTextRestClient, never()).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.UNEXPECTED_FAILURE), anyLong());
    }

    
}
