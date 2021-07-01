package uk.gov.companieshouse.ocr.api.image.extracttext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Pojo used in a Web Request sent by a client to this microservice
 */
public class OcrClientRequest {

    @JsonProperty("app_id")
    private String applicationId;

    @JsonProperty("response_id")
    private String responseId;

    @JsonProperty("image_endpoint")
    private String imageEndpoint;

    @JsonProperty("converted_text_endpoint")
    private String convertedTextEndpoint;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getImageEndpoint() {
        return imageEndpoint;
    }

    public void setImageEndpoint(String imageEndpoint) {
        this.imageEndpoint = imageEndpoint;
    }

    public String getConvertedTextEndpoint() {
        return convertedTextEndpoint;
    }

    public void setConvertedTextEndpoint(String convertedTextEndpoint) {
        this.convertedTextEndpoint = convertedTextEndpoint;
    }
    
}
