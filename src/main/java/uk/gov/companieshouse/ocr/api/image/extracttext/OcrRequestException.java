package uk.gov.companieshouse.ocr.api.image.extracttext;

public class OcrRequestException extends Exception {

    public enum ResultCode {
        FAIL_TO_GET_IMAGE(1),
        FAIL_TO_COVERT_IMAGE_TO_TEXT(2),
        UNEXPECTED_FAILURE(3),

        FAIL_TO_SEND_RESULTS(99); // Value just used for logging since we failed to send client call failed


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
