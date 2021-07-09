package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import uk.gov.companieshouse.ocr.api.TestObjectMother;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
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
        when(imageOcrService.extractTextFromImageBytesOld(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class)))
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

        // when
        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint()))
            .thenThrow(new OcrRequestException("Mock failure", OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE, new IOException("IOException test")));

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(imageOcrService, never()).extractTextFromImageBytesOld(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class));
        verify(callbackExtractedTextRestClient, never()).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE), anyLong());
        verify(monitoringService).logFailure(eq(ocrRequest.getContextId()), anyLong(), eq(OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE), eq(CallTypeEnum.ASYNCHRONOUS), eq(0));
    }

    @Test
    void failImageToText() throws OcrRequestException, IOException {

        // given
        CompletableFuture<TextConversionResult> futureExpection = new CompletableFuture<>();
        futureExpection.completeExceptionally(new IOException("Image to text failure test"));

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint())).thenReturn(MOCK_TIFF_CONTENT);
        when(imageOcrService.extractTextFromImageBytesOld(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class)))
           .thenReturn(futureExpection);

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(callbackExtractedTextRestClient, never()).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT), anyLong());
        verify(monitoringService).logFailure(eq(ocrRequest.getContextId()), anyLong(), eq(OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT), eq(CallTypeEnum.ASYNCHRONOUS), eq(MOCK_TIFF_CONTENT.length));
    }


    @Test
    void failSendingResults() throws OcrRequestException, IOException {

        // given
        var textConversionResult = TestObjectMother.getStandardTextConversionResult();
        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint())).thenReturn(MOCK_TIFF_CONTENT);
        when(imageOcrService.extractTextFromImageBytesOld(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class)))
           .thenReturn(CompletableFuture.completedFuture(textConversionResult));
        doThrow(new OcrRequestException("Mock failure", OcrRequestException.ResultCode.FAIL_TO_SEND_RESULTS, new HttpServerErrorException(HttpStatus.NOT_FOUND)))
           .when(callbackExtractedTextRestClient).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(callbackExtractedTextRestClient).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient, never()).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT), anyLong());
        verify(monitoringService).logFailToSendResults(eq(ocrRequest.getContextId()),any(MonitoringFields.class));
    }


    @Test
    void unexpectedException() throws OcrRequestException, IOException {

        when(imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint()))
            .thenThrow(new RuntimeException("Unexpected runtime exception"));

        // when
        ocrRequestService.handleRequest(ocrRequest, startedOcrRequestStopWatch()); 

        // then do no further processing and send error results
        verify(imageOcrService, never()).extractTextFromImageBytesOld(eq(ocrRequest.getContextId()), eq(MOCK_TIFF_CONTENT), eq(ocrRequest.getResponseId()), any (StopWatch.class));
        verify(callbackExtractedTextRestClient, never()).sendTextResult(eq(ocrRequest.getConvertedTextEndpoint()), any(ExtractTextResultDto.class));
        verify(callbackExtractedTextRestClient).sendTextResultError(eq(ocrRequest.getContextId()), eq(ocrRequest.getResponseId()), eq(ocrRequest.getConvertedTextEndpoint()), eq(OcrRequestException.ResultCode.UNEXPECTED_FAILURE), anyLong());
        verify(monitoringService).logFailure(eq(ocrRequest.getContextId()), anyLong(), eq(OcrRequestException.ResultCode.UNEXPECTED_FAILURE), eq(CallTypeEnum.ASYNCHRONOUS), eq(0));
    }

    
}
