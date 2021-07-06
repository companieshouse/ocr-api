package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.ThreadConfig;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;

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

    private ImageOcrTransformer transformer = new ImageOcrTransformer();

    @Async(ThreadConfig.OCR_REQUEST_EXECUTOR_BEAN)
    public void handleRequest(OcrRequest ocrRequest, StopWatch ocrRequestStopWatch) throws IOException {
        
        try {

            var logDataMap = ocrRequest.toMap();
            logDataMap.put("threadName",  Thread.currentThread().getName());

            LOG.infoContext(ocrRequest.getContextId(), "Orchestrating OCR Request", logDataMap);

            byte[] imageBytes = imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint());

            var timeOnQueueStopWatch = new StopWatch();
            timeOnQueueStopWatch.start();

            var textConversionResult =  imageOcrService.extractTextFromImageBytes(ocrRequest.getContextId(), imageBytes, ocrRequest.getResponseId(), timeOnQueueStopWatch).join();
  
            var extractTextResult =  transformer.mapModelToApi(textConversionResult);
            extractTextResult.setTotalProcessingTimeMs(ocrRequestStopWatch.getTime()); // for callback

            callbackExtractedTextRestClient.sendTextResult(ocrRequest.getConvertedTextEndpoint(), extractTextResult);

            ocrRequestStopWatch.stop();
            extractTextResult.setTotalProcessingTimeMs(ocrRequestStopWatch.getTime()); // Set this first time to get the full time in monitoring field
            var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult, CallTypeEnum.ASYNCHRONOUS);
    
            LOG.infoContext(ocrRequest.getContextId(), "Completed OCR Request - time to run " + (ocrRequestStopWatch.getTime()) + " (ms) " + "[ " +
            ocrRequestStopWatch.toString() + "]", monitoringFields.toMap());

        }
        catch(CompletionException ce) {
            LOG.errorContext(ocrRequest.getContextId(), "Error Converting image to text", ce, null); 
            callbackExtractedTextRestClient.sendTextResultError(ocrRequest.getContextId(), ocrRequest.getResponseId(), ocrRequest.getConvertedTextEndpoint(), OcrRequestException.ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT, stopStopWatchAndReturnTime(ocrRequestStopWatch));

        }
        catch (OcrRequestException e) {
            LOG.errorContext(ocrRequest.getContextId(), "Error in OCR Request", e, null);
            if (e.getResultCode().equals(OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE)) {
                callbackExtractedTextRestClient.sendTextResultError(ocrRequest.getContextId(), ocrRequest.getResponseId(), ocrRequest.getConvertedTextEndpoint(), e.getResultCode(), stopStopWatchAndReturnTime(ocrRequestStopWatch));
            }
        }
        catch (Exception e) {
            LOG.errorContext(ocrRequest.getContextId(), "Unexpected Error in OCR Request", e, null);
            callbackExtractedTextRestClient.sendTextResultError(ocrRequest.getContextId(), ocrRequest.getResponseId(), ocrRequest.getConvertedTextEndpoint(), OcrRequestException.ResultCode.UNEXPECTED_FAILURE, stopStopWatchAndReturnTime(ocrRequestStopWatch));

        }
    }

    private long stopStopWatchAndReturnTime(StopWatch stopWatch) {
        stopWatch.stop();
        return stopWatch.getTime();
    }
}
