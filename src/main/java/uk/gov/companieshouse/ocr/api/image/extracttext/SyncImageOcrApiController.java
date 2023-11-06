package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.common.ErrorResponseDto;
import uk.gov.companieshouse.ocr.api.common.JsonConstants;
import uk.gov.companieshouse.ocr.api.common.OcrGeneralConstants;
import uk.gov.companieshouse.ocr.api.image.extracttext.OcrRequestException.ResultCode;

/*
 IOException and TextConversionException are not really thrown on the POST methods in this class since it can only come from
 an asynchronous method when it is caught via a CompletionException
 */
@RestController
public class SyncImageOcrApiController extends AbstractOcrApiController{

    public static final String TIFF_EXTRACT_TEXT_PARTIAL_URL = "/api/ocr/image/tiff/extractText";

    static final String RESPONSE_ID_REQUEST_PARAMETER_NAME = "responseId";
    static final String CONTEXT_ID_REQUEST_PARAMETER_NAME = "contextId";
    static final String FILE_REQUEST_PARAMETER_NAME = "file";

    static final String GENERAL_SERVICE_ERROR_MESSAGE = "Unexpected Error In OCR Conversion";
    static final String TEXT_CONVERSION_ERROR_MESSAGE = "Text Conversion Error In OCR Conversion";
    static final String CONTROLLER_ERROR_MESSAGE = "Unexpected Error Before OCR Conversion";
    static final String NO_REQUEST_BODY_ERROR_MESSAGE = "Request body is required";

    @Autowired
    private OcrRequestService ocrRequestService;

    private ImageOcrTransformer transformer = new ImageOcrTransformer();


    @PostMapping("${api.endpoint}" + TIFF_EXTRACT_TEXT_PARTIAL_URL)
    public @ResponseBody ResponseEntity<ExtractTextResultDto> extractTextFromTiff(
            @RequestParam(FILE_REQUEST_PARAMETER_NAME) MultipartFile file, 
            @RequestParam(RESPONSE_ID_REQUEST_PARAMETER_NAME) String responseId,
            @RequestParam(name = CONTEXT_ID_REQUEST_PARAMETER_NAME, required = false) String contextId,
            HttpServletRequest request
            ) throws IOException, TextConversionException, OcrRequestException {

        var controllerStopWatch = new StopWatch();
        controllerStopWatch.start();

        if (StringUtils.isBlank(contextId)) {
            contextId = responseId;
        }
        
        logClientRequest(contextId, createRequestMap(file, responseId, contextId), request, CallTypeEnum.SYNCHRONOUS);

        try {
            var timeOnQueueStopWatch = new StopWatch();
            timeOnQueueStopWatch.start();
            var textConversionResult = ocrRequestService.handleSynchronousRequest(contextId, file.getBytes(), responseId, timeOnQueueStopWatch).join();

            var extractTextResult = transformer.mapModelToApi(textConversionResult);

            controllerStopWatch.stop();
            extractTextResult.setTotalProcessingTimeMs(controllerStopWatch.getTime());

            var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult, CallTypeEnum.SYNCHRONOUS);
            monitoringService.logSuccess(contextId, monitoringFields);

            return new ResponseEntity<>(extractTextResult, HttpStatus.OK);
        } catch (TaskRejectedException te) {
            throw new OcrRequestException("Capacity is full and can not add a new request", ResultCode.APPLICATION_OVERLOADED, CallTypeEnum.SYNCHRONOUS, contextId, te);
        }
        catch (CompletionException ce) {
            throw new OcrRequestException("Error when converting Image to Text", ResultCode.FAIL_TO_COVERT_IMAGE_TO_TEXT, CallTypeEnum.SYNCHRONOUS, contextId, ce);
        }
    }

    private Map<String, Object> createRequestMap(MultipartFile file, String responseId, String contextId) {
        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put(JsonConstants.RESPONSE_ID, responseId);
        requestMap.put(JsonConstants.CONTEXT_ID, contextId);
        requestMap.put(JsonConstants.ORIGINAL_FILENAME_NAME, file.getOriginalFilename());
        requestMap.put(JsonConstants.FILE_CONTENT_TYPE_NAME, file.getContentType());
        return requestMap;
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> uncaughtException(Exception e) {

        LOG.error(null, e);

        var errorResponse = new ErrorResponseDto();
        errorResponse.setErrorMessage(CONTROLLER_ERROR_MESSAGE);
        monitoringService.logFailure(OcrGeneralConstants.UNKNOWN_CONTEXT, 0, ResultCode.INTERNAL_SERVER_ERROR, CallTypeEnum.SYNCHRONOUS, 0);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
