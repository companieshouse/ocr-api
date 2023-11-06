package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.common.ErrorResponseDto;
import uk.gov.companieshouse.ocr.api.common.JsonConstants;

public abstract class AbstractOcrApiController {

    protected static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Autowired
    protected MonitoringService monitoringService;

    @ExceptionHandler(OcrRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleOcrRequestException(OcrRequestException ore) {

        LOG.errorContext(ore.getContextId(), ore.getMessage(), ore, null);

        monitoringService.logFailure(ore.getContextId(), 0, ore.getResultCode(), ore.getCallType(), 0);

        var errorResponse = new ErrorResponseDto();
        errorResponse.setContextId(ore.getContextId());
        errorResponse.setErrorMessage(ore.getResultCode().getMessage());

        return new ResponseEntity<>(errorResponse, ore.getResultCode().getHttpStatus());
    }

    protected void logClientRequest(String contextId, Map<String, Object> requestMap, HttpServletRequest request, CallTypeEnum callType) {

        requestMap.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.HTTP_REQUEST_LOG_RECORD);
        requestMap.put(JsonConstants.CALL_TYPE_NAME, callType.getFieldValue());
        requestMap.put(JsonConstants.CLIENT_IP_NAME, request.getRemoteAddr());
        requestMap.put(JsonConstants.HTTP_REFERER_NAME, request.getHeader("referer") != null ? request.getHeader("referer") : "null");
        LOG.infoContext(contextId,"Received OCR request [" + callType.getFieldValue() + "]", requestMap);       
    }
    
}