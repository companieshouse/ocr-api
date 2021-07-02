package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.LinkedHashMap;

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

    @Async(ThreadConfig.OCR_REQUEST_EXECUTOR_BEAN)
    public void handleRequest(OcrRequest ocrRequest) {

        try {

            var logDataMap = ocrRequest.toMap();
            logDataMap.put("threadName",  Thread.currentThread().getName());

            LOG.infoContext(ocrRequest.getContextId(), "Orchestrating OCR Request", logDataMap);

            byte[] image = imageRestClient.getImageContentsFromEndpoint(ocrRequest.getContextId(), ocrRequest.getImageEndpoint());

            logDataMap = new LinkedHashMap<>();
            logDataMap.put("fileSize", image.length);
            LOG.infoContext(ocrRequest.getContextId(), "Completed OCR Request", logDataMap);

        }
        catch (OcrRequestException e) {
            LOG.errorContext(ocrRequest.getContextId(), "Error in OCR Request", e, null);

        }


        
    }
}
