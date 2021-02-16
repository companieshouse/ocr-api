package uk.gov.companieshouse.ocr.api.image.extracttext;

public class TextConversionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String responseId;
    private final String contextId;

    public TextConversionException(String contextId, String responseId, Throwable cause) {
        super(cause);
        this.contextId = contextId;
        this.responseId = responseId;
    }
    
    public String getContextId() {
        return contextId;
    }

    public String getResponseId() {
        return responseId;
    }

    
}
