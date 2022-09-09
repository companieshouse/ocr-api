package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.companieshouse.ocr.api.SpringConfiguration;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.common.ErrorResponseDto;
import uk.gov.companieshouse.ocr.api.common.OcrGeneralConstants;
import uk.gov.companieshouse.ocr.api.image.extracttext.OcrRequestException.ResultCode;
import uk.gov.companieshouse.ocr.api.urlValidator.UrlValidatorException;
import uk.gov.companieshouse.ocr.api.urlValidator.WhiteListedUrlValidator;


@RestController
public class AsyncImageOcrApiController extends AbstractOcrApiController {

    public static final String TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL = "/api/ocr/image/tiff/extractTextRequest";

    static final String RESPONSE_ID_REQUEST_PARAMETER_NAME = "responseId";
    static final String CONTEXT_ID_REQUEST_PARAMETER_NAME = "contextId";
    static final String FILE_REQUEST_PARAMETER_NAME = "file";

    static final String GENERAL_SERVICE_ERROR_MESSAGE = "Unexpected Error In OCR Conversion";
    static final String TEXT_CONVERSION_ERROR_MESSAGE = "Text Conversion Error In OCR Conversion";
    static final String CONTROLLER_ERROR_MESSAGE = "Unexpected Error Before OCR Conversion";
    static final String NO_REQUEST_BODY_ERROR_MESSAGE = "Request body is required";

    private static final String INVALID_INPUT_ERROR_MESSAGE = "invalid input";
    private static final String UNKNOWN = "UNKNOWN";


    private OcrRequestService ocrRequestService;

    private WhiteListedUrlValidator whiteListedUrlValidator;

    @Autowired
    public AsyncImageOcrApiController(SpringConfiguration springConfiguration, OcrRequestService ocrRequestService) {
        this.whiteListedUrlValidator = new WhiteListedUrlValidator(springConfiguration.getHostWhiteList());
        this.ocrRequestService = ocrRequestService;
    }

    @PostMapping("${api.endpoint}" + TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL)
    public ResponseEntity<HttpStatus> receiveOcrRequest(@Valid @RequestBody OcrClientRequest clientRequest, HttpServletRequest request) throws OcrRequestException {

        var ocrRequestStopWatch = new StopWatch();
        ocrRequestStopWatch.start();

        OcrRequest ocrRequest = new OcrRequest(clientRequest, LocalDateTime.now());
        logClientRequest(ocrRequest.getContextId(), clientRequest.toMap(), request, CallTypeEnum.ASYNCHRONOUS);

        try {
          whiteListedUrlValidator.validateUrl(ocrRequest.getImageEndpoint());
          whiteListedUrlValidator.validateUrl(ocrRequest.getConvertedTextEndpoint());
        }
        catch (UrlValidatorException ve) {
          throw new OcrRequestException(ve.getMessage(), ResultCode.BAD_REQUEST,CallTypeEnum.ASYNCHRONOUS,ocrRequest.getContextId(),ve);
        }

        try {
           ocrRequestService.handleAsynchronousRequest(ocrRequest, ocrRequestStopWatch);
        }
        catch (TaskRejectedException te) {
            throw new OcrRequestException("Capacity is full and can not add a new request", ResultCode.APPLICATION_OVERLOADED, CallTypeEnum.ASYNCHRONOUS, ocrRequest.getContextId(), te);
        }

        LOG.infoContext(ocrRequest.getContextId(),"OCR request now being handled asynchronously", null);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleCompletionException(MethodArgumentNotValidException ex)  {
        
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
            
        var errorResponse = new ErrorResponseDto();

        errorResponse.setErrorMessage(INVALID_INPUT_ERROR_MESSAGE + errors.toString());
        errorResponse.setContextId(UNKNOWN);
        errorResponse.setResponseId(UNKNOWN);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("client-input-errors", errors);
        LOG.info(INVALID_INPUT_ERROR_MESSAGE, map);

        monitoringService.logFailure(OcrGeneralConstants.UNKNOWN_CONTEXT, 0, ResultCode.BAD_REQUEST, CallTypeEnum.ASYNCHRONOUS, 0);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    } 


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {

        var errorResponse = new ErrorResponseDto();

        errorResponse.setErrorMessage(NO_REQUEST_BODY_ERROR_MESSAGE);
        errorResponse.setContextId(UNKNOWN);
        errorResponse.setResponseId(UNKNOWN);

        LOG.info(NO_REQUEST_BODY_ERROR_MESSAGE, null);

        monitoringService.logFailure(OcrGeneralConstants.UNKNOWN_CONTEXT, 0, ResultCode.BAD_REQUEST, CallTypeEnum.ASYNCHRONOUS, 0);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> uncaughtException(Exception e) {

        LOG.error(null, e);

        var errorResponse = new ErrorResponseDto();
        errorResponse.setErrorMessage(CONTROLLER_ERROR_MESSAGE);

        monitoringService.logFailure(OcrGeneralConstants.UNKNOWN_CONTEXT, 0, ResultCode.INTERNAL_SERVER_ERROR, CallTypeEnum.ASYNCHRONOUS, 0);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
