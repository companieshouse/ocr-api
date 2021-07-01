package uk.gov.companieshouse.ocr.api.image.extracttext;

public class OcrRequestException extends Exception {

    public enum ResultCode {
        FAIL_TO_GET_IMAGE(1);

        private final int code;

        private ResultCode(int code) {
             this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }

    private final ResultCode resultCode;

    public OcrRequestException(String message, ResultCode resultCode, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    
}
