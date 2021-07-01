package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.IOException;
import java.util.LinkedHashMap;
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

    private ImageOcrTransformer transformer = new ImageOcrTransformer();

    @Async(ThreadConfig.OCR_REQUEST_EXECUTOR_BEAN)
    public void handleRequest(OcrRequest ocrRequest) {

        var controllerStopWatch = new StopWatch();
        controllerStopWatch.start();
        
        try {

            var logDataMap = ocrRequest.toMap();
            logDataMap.put("threadName",  Thread.currentThread().getName());

            LOG.infoContext(ocrRequest.getContextId(), "Orchestrating OCR Request", logDataMap);

            byte[] imageBytes = imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint());

            var timeOnQueueStopWatch = new StopWatch();
            timeOnQueueStopWatch.start();

            try {
               var textConversionResult =  imageOcrService.extractTextFromImageBytes(ocrRequest.getContextId(), imageBytes, ocrRequest.getResponseId(), timeOnQueueStopWatch).join();
               LOG.infoContext(ocrRequest.getContextId(), "File converted[" + textConversionResult.getExtractedText() +"]", textConversionResult.metaDataMap());
            } 
            catch(CompletionException ce) {
                LOG.errorContext(ocrRequest.getContextId(), "Error Converting image to text", ce, null); 
            }
            catch(IOException io) { // for compiler since we are using a join on an async method
                LOG.errorContext(ocrRequest.getContextId(), "IO Error Converting image to text", io, null); 
            }
    /*
            var extractTextResult =  transformer.mapModelToApi(textConversionResult);
    
            var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult);
    
            LOG.infoContext(contextId, "Finished file " + file.getOriginalFilename() + " - time to run " + (controllerStopWatch.getTime()) + " (ms) " + "[ " +
               controllerStopWatch.toString() + "]", monitoringFields.toMap());
*/
            logDataMap = new LinkedHashMap<>();
            logDataMap.put("fileSize", imageBytes.length);
            LOG.infoContext(ocrRequest.getContextId(), "Completed OCR Request", logDataMap);

        }
        catch (OcrRequestException e) {
            LOG.errorContext(ocrRequest.getContextId(), "Error in OCR Request", e, null);

        }


        
    }
}
