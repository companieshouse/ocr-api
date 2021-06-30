package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.time.LocalDateTime;

import org.apache.commons.lang.StringUtils;

/**
 * OcrRequest has the data required when processing an ocr request within this application
 */
public class OcrRequest {

    private final String applicationID;

    private final String responseID;

    private final String imageEndpoint;

    private final String convertedTextEndpoint;
    
    private final String contextId;

    private final LocalDateTime timeRequestReceived;

    public OcrRequest(OcrClientRequest clientRequest, LocalDateTime timeRequestReceived) {
        applicationID = StringUtils.trim(clientRequest.getApplicationID());
        responseID = StringUtils.trim(clientRequest.getResponseID());
        imageEndpoint = StringUtils.trim(clientRequest.getImageEndpoint());
        convertedTextEndpoint = StringUtils.trim(clientRequest.getConvertedTextEndpoint());
        contextId = applicationID + '-' + responseID;
        this.timeRequestReceived = timeRequestReceived;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public String getResponseID() {
        return responseID;
    }

    public String getImageEndpoint() {
        return imageEndpoint;
    }

    public String getConvertedTextEndpoint() {
        return convertedTextEndpoint;
    }

    public String getContextId() {
        return contextId;
    }

    public LocalDateTime getTimeRequestReceived() {
        return timeRequestReceived;
    }

}