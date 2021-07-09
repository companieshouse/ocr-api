package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.common.ErrorResponseDto;
import uk.gov.companieshouse.ocr.api.common.OcrGeneralConstants;
import uk.gov.companieshouse.ocr.api.image.extracttext.OcrRequestException.ResultCode;

/*
 IOException is not really thrown on the POST methods in this class since it can only come from
 an asynchronous method when it is caught via a CompletionException
 */
@RestController
public class SyncImageOcrApiController {

    public static final String TIFF_EXTRACT_TEXT_PARTIAL_URL = "/api/ocr/image/tiff/extractText";

    static final String RESPONSE_ID_REQUEST_PARAMETER_NAME = "responseId";
    static final String CONTEXT_ID_REQUEST_PARAMETER_NAME = "contextId";
    static final String FILE_REQUEST_PARAMETER_NAME = "file";

    static final String GENERAL_SERVICE_ERROR_MESSAGE = "Unexpected Error In OCR Conversion";
    static final String TEXT_CONVERSION_ERROR_MESSAGE = "Text Conversion Error In OCR Conversion";
    static final String CONTROLLER_ERROR_MESSAGE = "Unexpected Error Before OCR Conversion";
    static final String NO_REQUEST_BODY_ERROR_MESSAGE = "Request body is required";

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Autowired
    private ImageOcrService imageOcrService;

    @Autowired
    private MonitoringService monitoringService;

    private ImageOcrTransformer transformer = new ImageOcrTransformer();


    @PostMapping("${api.endpoint}" + TIFF_EXTRACT_TEXT_PARTIAL_URL)
    public @ResponseBody ResponseEntity<ExtractTextResultDto> extractTextFromTiff(
            @RequestParam(FILE_REQUEST_PARAMETER_NAME) MultipartFile file, 
            @RequestParam(RESPONSE_ID_REQUEST_PARAMETER_NAME) String responseId,
            @RequestParam(name = CONTEXT_ID_REQUEST_PARAMETER_NAME, required = false) String contextId
            ) throws IOException {

        var controllerStopWatch = new StopWatch();
        controllerStopWatch.start();

        if (StringUtils.isBlank(contextId)) {
            contextId = responseId;
        }
        
        LOG.infoContext(contextId,"Received from client file [" + file.getOriginalFilename() + "] Content type [" + file.getContentType() + "]", null);

        var timeOnQueueStopWatch = new StopWatch();
        timeOnQueueStopWatch.start();
        var textConversionResult =  imageOcrService.extractTextFromImageBytesOld(contextId, file.getBytes(), responseId, timeOnQueueStopWatch).join();

        var extractTextResult =  transformer.mapModelToApi(textConversionResult);

        controllerStopWatch.stop();
        extractTextResult.setTotalProcessingTimeMs(controllerStopWatch.getTime());

        var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult, CallTypeEnum.SYNCHRONOUS);
        monitoringService.logSuccess(contextId, monitoringFields);

        return new ResponseEntity<>(extractTextResult, HttpStatus.OK);
    }

    /*
     Occurs when the  `.join()` method is called after calling an `async` method AND an untrapped exception is thown within that method
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ErrorResponseDto> handleCompletionException(CompletionException e) {

        var errorResponse = new ErrorResponseDto();
        if (e.getCause() instanceof TextConversionException) {

            var cause = (TextConversionException) e.getCause();
            logError(cause.getContextId(), cause);
            errorResponse.setErrorMessage(TEXT_CONVERSION_ERROR_MESSAGE);
            errorResponse.setContextId(cause.getContextId());
            errorResponse.setResponseId(cause.getResponseId());
            monitoringService.logFailure(cause.getContextId(), 0, ResultCode.INTERNAL_SERVER_ERROR, CallTypeEnum.SYNCHRONOUS, 0);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else {
            logError(null, e);
            errorResponse.setErrorMessage(GENERAL_SERVICE_ERROR_MESSAGE);
            monitoringService.logFailure(OcrGeneralConstants.UNKNOWN_CONTEXT, 0, ResultCode.INTERNAL_SERVER_ERROR, CallTypeEnum.SYNCHRONOUS, 0);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> uncaughtException(Exception e) {

        LOG.error(null, e);

        var errorResponse = new ErrorResponseDto();
        errorResponse.setErrorMessage(CONTROLLER_ERROR_MESSAGE);
        monitoringService.logFailure(OcrGeneralConstants.UNKNOWN_CONTEXT, 0, ResultCode.INTERNAL_SERVER_ERROR, CallTypeEnum.SYNCHRONOUS, 0);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logError(String responseId, Exception e) {

        LOG.errorContext(responseId,  e, null);
    }

}
