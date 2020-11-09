package uk.gov.companieshouse.ocr.api.image.extracttext;

public class TextConversionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String responseId;

    public TextConversionException(String responseId, Throwable cause) {
        super(cause);
        this.responseId = responseId;
    }

    public String getResponseId() {
        return responseId;
    } 
    
}
