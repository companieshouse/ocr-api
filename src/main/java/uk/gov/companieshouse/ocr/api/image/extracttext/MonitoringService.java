package uk.gov.companieshouse.ocr.api.image.extracttext;

import org.springframework.stereotype.Service;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;

@Service
public class MonitoringService {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    private static final String ERROR_MESSAGE = "OCR Request Completed In Error";


    public void logSuccess(String contextId, MonitoringFields monitoringFields) {
        LOG.infoContext(contextId, "OCR Request Completed Successfully" , monitoringFields.toMap());
    }

    public void logFailToSendResults(String contextId, MonitoringFields monitoringFields) {
        LOG.infoContext(contextId, ERROR_MESSAGE , monitoringFields.toMap());
    }

    public void logFailure(String contextId, long totalProcessingTimeMs, OcrRequestException.ResultCode resultCode, CallTypeEnum callType, int imageSize) {

        MonitoringFields monitoringFields =  new MonitoringFields(totalProcessingTimeMs, resultCode, callType, imageSize);
        LOG.infoContext(contextId, ERROR_MESSAGE , monitoringFields.toMap());
    }
    
}