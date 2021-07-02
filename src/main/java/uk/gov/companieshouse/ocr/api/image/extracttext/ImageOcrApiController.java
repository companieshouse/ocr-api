package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.common.ErrorResponseDto;

/*
 IOException is not really thrown on the POST methods in this class since it can only come from
 an asynchronous method when it is caught via a CompletionException
 */
@RequestMapping("${api.endpoint}")
@RestController
public class ImageOcrApiController {

    public static final String TIFF_EXTRACT_TEXT_PARTIAL_URL = "/api/ocr/image/tiff/extractText";
    public static final String TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL = "/api/ocr/image/tiff/extractTextRequest";

    static final String RESPONSE_ID_REQUEST_PARAMETER_NAME = "responseId";
    static final String CONTEXT_ID_REQUEST_PARAMETER_NAME = "contextId";
    static final String FILE_REQUEST_PARAMETER_NAME = "file";

    static final String GENERAL_SERVICE_ERROR_MESSAGE = "Unexpected Error In OCR Conversion";
    static final String TEXT_CONVERSION_ERROR_MESSAGE = "Text Conversion Error In OCR Conversion";
    static final String CONTROLLER_ERROR_MESSAGE = "Unexpected Error Before OCR Conversion";

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Autowired
    private ImageOcrService imageOcrService;

    @Autowired
    private OcrRequestService ocrRequestService;

    private ImageOcrTransformer transformer = new ImageOcrTransformer();


    @PostMapping(TIFF_EXTRACT_TEXT_REQUEST_PARTIAL_URL)
    public ResponseEntity<HttpStatus> receiveOcrRequest(@RequestBody OcrClientRequest clientRequest) throws IOException {

        var ocrRequestStopWatch = new StopWatch();
        ocrRequestStopWatch.start();

        OcrRequest ocrRequest = new OcrRequest(clientRequest, LocalDateTime.now());
        LOG.infoContext(ocrRequest.getContextId(),"Received OCR request", null);

        ocrRequestService.handleRequest(ocrRequest, ocrRequestStopWatch);

        LOG.infoContext(ocrRequest.getContextId(),"OCR request now being handled asynchronously", null);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }


    @PostMapping(TIFF_EXTRACT_TEXT_PARTIAL_URL)
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
        var textConversionResult =  imageOcrService.extractTextFromImageBytes(contextId, file.getBytes(), responseId, timeOnQueueStopWatch).join();

        var extractTextResult =  transformer.mapModelToApi(textConversionResult);

        controllerStopWatch.stop();
        extractTextResult.setTotalProcessingTimeMs(controllerStopWatch.getTime());

        var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult);
    
        LOG.infoContext(contextId, "Finished file " + file.getOriginalFilename() + " - time to run " + (controllerStopWatch.getTime()) + " (ms) " + "[ " +
           controllerStopWatch.toString() + "]", monitoringFields.toMap());

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
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else {
            logError(null, e);
            errorResponse.setErrorMessage(GENERAL_SERVICE_ERROR_MESSAGE);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> uncaughtException(Exception e) {

        LOG.error(null, e);

        var errorResponse = new ErrorResponseDto();
        errorResponse.setErrorMessage(CONTROLLER_ERROR_MESSAGE);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logError(String responseId, Exception e) {

        LOG.errorContext(responseId,  e, null);
    }

}
