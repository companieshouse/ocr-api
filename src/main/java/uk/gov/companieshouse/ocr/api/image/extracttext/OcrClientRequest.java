package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Pojo used in a Web Request sent by a client to this microservice
 */
public class OcrClientRequest {

    @NotBlank(message = "Missing required value app_id")
    @JsonProperty("app_id")
    private String applicationId;

    @NotBlank(message = "Missing required value response_id")
    @JsonProperty("response_id")
    private String responseId;

    @NotBlank(message = "Missing required value image_endpoint")
    @JsonProperty("image_endpoint")
    private String imageEndpoint;

    @NotBlank(message = "Missing required value converted_text_endpoint")
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

    public Map<String, Object> toMap() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("applicationId", applicationId);
        map.put("responseId", responseId);
        map.put("imageEndpoint", imageEndpoint);
        map.put("convertedTextEndpoint", convertedTextEndpoint);

        return  map;        
    }
    
}
