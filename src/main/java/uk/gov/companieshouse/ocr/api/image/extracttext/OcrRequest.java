package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * OcrRequest has the data required when processing an ocr request within this application
 */
public class OcrRequest {

    private final String applicationId;

    private final String responseId;

    private final String imageEndpoint;

    private final String convertedTextEndpoint;
    
    private final String contextId;

    private final LocalDateTime timeRequestReceived;

    public OcrRequest(OcrClientRequest clientRequest, LocalDateTime timeRequestReceived) {
        applicationId = StringUtils.trim(clientRequest.getApplicationId());
        responseId = StringUtils.trim(clientRequest.getResponseId());
        imageEndpoint = StringUtils.trim(clientRequest.getImageEndpoint());
        convertedTextEndpoint = StringUtils.trim(clientRequest.getConvertedTextEndpoint());
        contextId = applicationId + '-' + responseId;
        this.timeRequestReceived = timeRequestReceived;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getResponseId() {
        return responseId;
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

    public Map<String, Object> toMap() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("applicationId", applicationId);
        map.put("responseId", responseId);
        map.put("imageEndpoint", imageEndpoint);
        map.put("convertedTextEndpoint", convertedTextEndpoint);
        map.put("contextId", contextId);
        map.put("timeRequestReceived", timeRequestReceived);

        return  map;        
    }

}