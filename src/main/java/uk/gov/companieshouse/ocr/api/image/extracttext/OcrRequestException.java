package uk.gov.companieshouse.ocr.api.image.extracttext;


import org.springframework.http.HttpStatus;

public class OcrRequestException extends Exception {

    public enum ResultCode {
        FAIL_TO_GET_IMAGE(1),
        FAIL_TO_COVERT_IMAGE_TO_TEXT(2),
        UNEXPECTED_FAILURE(3),

        BAD_REQUEST(HttpStatus.BAD_REQUEST.value()),
        INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value()),

        // Values 990 - 999 just used for logging (e.g. we failed to send client call failed)
        FAIL_LOGIC_ERROR(998),
        FAIL_TO_SEND_RESULTS(999);


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
