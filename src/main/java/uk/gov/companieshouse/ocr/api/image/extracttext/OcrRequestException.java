package uk.gov.companieshouse.ocr.api.image.extracttext;


import org.springframework.http.HttpStatus;

import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;


public class OcrRequestException extends Exception {

    public enum ResultCode {
        FAIL_TO_GET_IMAGE(1, HttpStatus.INTERNAL_SERVER_ERROR, "Fail to get image"),
        FAIL_TO_COVERT_IMAGE_TO_TEXT(2, HttpStatus.INTERNAL_SERVER_ERROR, "Fail to convert Image to Text"),
        UNEXPECTED_FAILURE(3, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Failure"),

        BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST, "Bad OCR Request"),
        BAD_URL(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST, "Bad Url contained in OCR Request"),
        INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error"),
        APPLICATION_OVERLOADED(HttpStatus.SERVICE_UNAVAILABLE.value(), HttpStatus.SERVICE_UNAVAILABLE, "Service Overloaded"),

        // Values 990 - 999 just used for logging (e.g. we failed to send client call failed)
        FAIL_LOGIC_ERROR(998, HttpStatus.INTERNAL_SERVER_ERROR, "Logic Error in application"),
        FAIL_TO_SEND_RESULTS(999, HttpStatus.INTERNAL_SERVER_ERROR, "Fail to send results back to calling system");


        private final int code;
        private final HttpStatus httpStatus;
        private final String message;

        private ResultCode(int code, HttpStatus httpStatus, String message) {
             this.code = code;
             this.httpStatus = httpStatus;
             this.message = message;
        }

        public int getCode() {
            return this.code;
        }

        public HttpStatus getHttpStatus() {
            return httpStatus;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ResultCode resultCode;
    private final String contextId;
    private final CallTypeEnum callType;

    public OcrRequestException(String message, ResultCode resultCode, CallTypeEnum callType, String contextId, Throwable cause) {
        super(message, cause);
        this.contextId = contextId;
        this.resultCode = resultCode;
        this.callType = callType;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public String getContextId() {
        return contextId;
    }

    public CallTypeEnum getCallType() {
        return callType;
    }
    
}
