package uk.gov.companieshouse.ocr.api.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  The response to the client when an error occurs (the context and/or response id might not be present due to the asynchonous nature of the processing in this microservice)
 */
public class ErrorResponseDto {

    @JsonProperty("context_id")
    private String contextId;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("response_id")
    private String responseId;

    /*  ------ Accessors ------- */

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

}