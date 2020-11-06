package uk.gov.companieshouse.ocr.api.image.extracttext;

public class TextConversionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String externalReferenceId;

    public TextConversionException(String externalReferenceId, Throwable cause) {
        super(cause);
        this.externalReferenceId = externalReferenceId;
    }

    public String getExternalReferenceId() {
        return externalReferenceId;
    } 
    
}
