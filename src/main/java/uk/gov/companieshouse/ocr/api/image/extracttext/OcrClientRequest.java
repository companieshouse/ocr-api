package uk.gov.companieshouse.ocr.api.image.extracttext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Pojo used in a Web Request sent by a client to this microservice
 */
public class OcrClientRequest {

    @JsonProperty("app_id")
    private String applicationID;

    @JsonProperty("response_id")
    private String responseID;

    @JsonProperty("image_endpoint")
    private String imageEndpoint;

    @JsonProperty("converted_text_endpoint")
    private String convertedTextEndpoint;

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getResponseID() {
        return responseID;
    }

    public void setResponseID(String responseID) {
        this.responseID = responseID;
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
