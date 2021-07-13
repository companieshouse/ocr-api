package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.ThreadConfig;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.common.JsonConstants;

/**
 * This controls the workflow of an ocr request
 */
@Service
public class OcrRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Autowired
    private ImageRestClient imageRestClient;

    @Autowired
    private ImageOcrService imageOcrService;

    @Autowired
    private CallbackExtractedTextRestClient callbackExtractedTextRestClient;

    @Autowired
    private MonitoringService monitoringService;

    private ImageOcrTransformer transformer = new ImageOcrTransformer();

    @Async(ThreadConfig.OCR_REQUEST_EXECUTOR_BEAN)
    public void handleAsynchronousRequest(OcrRequest ocrRequest, StopWatch ocrRequestStopWatch)  {
        
        var timeOnQueue = ocrRequestStopWatch.getTime();
        byte[] imageBytes = {};
        ExtractTextResultDto extractTextResult = null;
        TextConversionResult textConversionResult = null;
        try {

            var logDataMap = createOcrRequestPostQueueLogMap(ocrRequest, timeOnQueue);
            LOG.infoContext(ocrRequest.getContextId(), "Orchestrating OCR Request From Asynchronous Request", logDataMap);

            imageBytes = imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint());

            textConversionResult =  imageOcrService.extractTextFromImageBytes(ocrRequest.getContextId(), imageBytes, ocrRequest.getResponseId(), timeOnQueue);
  
            extractTextResult =  transformer.mapModelToApi(textConversionResult);
            extractTextResult.setTotalProcessingTimeMs(ocrRequestStopWatch.getTime()); // set the time for the client

            callbackExtractedTextRestClient.sendTextResult(ocrRequest.getContextId(), ocrRequest.getConvertedTextEndpoint(), extractTextResult);

            ocrRequestStopWatch.stop();
            extractTextResult.setTotalProcessingTimeMs(ocrRequestStopWatch.getTime()); // Set the time for the monitoring field
            var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult, CallTypeEnum.ASYNCHRONOUS);

            monitoringService.logSuccess(ocrRequest.getContextId(), monitoringFields);
    
        }
        catch(IOException | TextConversionException e) {
            var totalProcessingTimeMs = stopStopWatchAndReturnTime(ocrRequestStopWatch);
            LOG.errorContext(ocrRequest.getContextId(), "Error Converting image to text", e, null); 
            callbackExtractedTextRestClient.sendTextResultError(ocrRequest.getContextId(), ocrRequest.getResponseId(), ocrRequest.getConvertedTextEndpoint(), OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT, totalProcessingTimeMs);
            monitoringService.logFailure(ocrRequest.getContextId(), totalProcessingTimeMs, OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT,  CallTypeEnum.ASYNCHRONOUS, imageBytes.length);

        }
        catch (OcrRequestException e) {
            LOG.errorContext(ocrRequest.getContextId(), "Error in OCR Request", e, null);
            var totalProcessingTimeMs = stopStopWatchAndReturnTime(ocrRequestStopWatch);
            switch(e.getResultCode()) {
                case FAIL_TO_GET_IMAGE:
                    callbackExtractedTextRestClient.sendTextResultError(ocrRequest.getContextId(), ocrRequest.getResponseId(), ocrRequest.getConvertedTextEndpoint(), e.getResultCode(), totalProcessingTimeMs);
                    monitoringService.logFailure(ocrRequest.getContextId(), totalProcessingTimeMs, e.getResultCode(),  CallTypeEnum.ASYNCHRONOUS, imageBytes.length);
                    break;
                case FAIL_TO_SEND_RESULTS:
                    if (extractTextResult != null && textConversionResult != null) {
                        extractTextResult.setTotalProcessingTimeMs(totalProcessingTimeMs); 
                        extractTextResult.setResultCode(e.getResultCode().getCode());
                        var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult, CallTypeEnum.ASYNCHRONOUS);
                        monitoringService.logFailToSendResults(ocrRequest.getContextId(), monitoringFields);
                    }
                    else {
                        monitoringService.logFailure(ocrRequest.getContextId(), totalProcessingTimeMs, e.getResultCode(),  CallTypeEnum.ASYNCHRONOUS, imageBytes.length);             
                    }
                    break;
                default:
                    callbackExtractedTextRestClient.sendTextResultError(ocrRequest.getContextId(), ocrRequest.getResponseId(), ocrRequest.getConvertedTextEndpoint(), e.getResultCode(), totalProcessingTimeMs);
                    monitoringService.logFailure(ocrRequest.getContextId(), totalProcessingTimeMs, e.getResultCode(),  CallTypeEnum.ASYNCHRONOUS, imageBytes.length);                               
            }
        }
        catch (Exception e) {
            LOG.errorContext(ocrRequest.getContextId(), "Unexpected Error in OCR Request", e, null);
            var totalProcessingTimeMs = stopStopWatchAndReturnTime(ocrRequestStopWatch);
            OcrRequestException.ResultCode unexpectedFailureResultCode = OcrRequestException.ResultCode.UNEXPECTED_FAILURE;
            callbackExtractedTextRestClient.sendTextResultError(ocrRequest.getContextId(), ocrRequest.getResponseId(), ocrRequest.getConvertedTextEndpoint(),  unexpectedFailureResultCode, totalProcessingTimeMs);
            monitoringService.logFailure(ocrRequest.getContextId(), totalProcessingTimeMs, unexpectedFailureResultCode,  CallTypeEnum.ASYNCHRONOUS, imageBytes.length);             
        }
    }

    private Map<String, Object> createOcrRequestPostQueueLogMap(OcrRequest ocrRequest, long timeOnQueue) {
        var logDataMap = ocrRequest.toMap();
        logDataMap.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.POST_QUEUE_LOG_RECORD);
        logDataMap.put(JsonConstants.THREAD_NAME_NAME, Thread.currentThread().getName());
        logDataMap.put(JsonConstants.TIME_ON_QUEUE_MS_NAME, timeOnQueue);
        logDataMap.put(JsonConstants.CALL_TYPE_NAME, CallTypeEnum.ASYNCHRONOUS.getFieldValue());

        return logDataMap;
    }

    @Async(ThreadConfig.OCR_REQUEST_EXECUTOR_BEAN)
    public CompletableFuture<TextConversionResult> 
    handleSynchronousRequest(String contextId, byte[] imageBytes, String responseId, StopWatch timeOnQueueStopWatch) throws IOException, TextConversionException {

        var timeOnQueue = stopStopWatchAndReturnTime(timeOnQueueStopWatch);

        var logDataMap = createSyncPostQueueLogMap(timeOnQueue);
        LOG.infoContext(contextId, "Continuing Synchronous Request", logDataMap);

        var textConversionResult =  imageOcrService.extractTextFromImageBytes(contextId, imageBytes, responseId, timeOnQueue);

        return CompletableFuture.completedFuture(textConversionResult);
    }

    private Map<String, Object> createSyncPostQueueLogMap(long timeOnQueue) {
        Map<String, Object> logDataMap = new LinkedHashMap<>();
        logDataMap.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.POST_QUEUE_LOG_RECORD);
        logDataMap.put(JsonConstants.THREAD_NAME_NAME, Thread.currentThread().getName());
        logDataMap.put(JsonConstants.TIME_ON_QUEUE_MS_NAME, timeOnQueue);
        logDataMap.put(JsonConstants.CALL_TYPE_NAME, CallTypeEnum.SYNCHRONOUS.getFieldValue());

        return logDataMap;
    }

    private long stopStopWatchAndReturnTime(StopWatch stopWatch) {
        stopWatch.stop();
        return stopWatch.getTime();
    }
}
