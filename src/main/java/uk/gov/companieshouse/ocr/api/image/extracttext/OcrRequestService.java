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
            LOG.infoContext(ocrRequest.getContextId(), "File converted[" + textConversionResult.getExtractedText() +"]", textConversionResult.metaDataMap());
  
            var extractTextResult =  transformer.mapModelToApi(textConversionResult);

            callbackExtractedTextRestClient.sendTextResult(ocrRequest.getConvertedTextEndpoint(), extractTextResult);

            ocrRequestStopWatch.stop();
            extractTextResult.setTotalProcessingTimeMs(ocrRequestStopWatch.getTime());
    
            var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult);
    
            LOG.infoContext(ocrRequest.getContextId(), "Completed OCR Request - time to run " + (ocrRequestStopWatch.getTime()) + " (ms) " + "[ " +
            ocrRequestStopWatch.toString() + "]", monitoringFields.toMap());

        }
        catch(CompletionException ce) {
            LOG.errorContext(ocrRequest.getContextId(), "Error Converting image to text", ce, null); 
        }
        catch (OcrRequestException e) {
            LOG.errorContext(ocrRequest.getContextId(), "Error in OCR Request", e, null);
        }


        
    }
}
