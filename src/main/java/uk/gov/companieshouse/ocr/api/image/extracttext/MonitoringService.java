package uk.gov.companieshouse.ocr.api.image.extracttext;

import org.springframework.stereotype.Service;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;

@Service
public class MonitoringService {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);


    public void logSuccess(OcrRequest ocrRequest, MonitoringFields monitoringFields) {
        LOG.infoContext(ocrRequest.getContextId(), "Completed OCR Request Successfully" , monitoringFields.toMap());
    }
    
}