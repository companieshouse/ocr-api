package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;

@Component
public class CallbackExtractedTextRestClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Sends the extracted text to the extracted text endpoint.
     * @param   contextId               The context of the request.
     * @param   extractedTextEndpoint   The endpoint to send the extracted text to.
     * @param   extractedText           The extracted text DTO object.
     * @throws OcrRequestException
     */
    public void sendTextResult(String contextId, String extractedTextEndpoint, ExtractTextResultDto extractedText) throws OcrRequestException {
        
        try {
            HttpEntity<ExtractTextResultDto> entity = new HttpEntity<>(extractedText);
            restTemplate.postForEntity(extractedTextEndpoint, entity, String.class);

        } catch (Exception e) {
            throw new OcrRequestException(
                "Fail to send results back to calling application at url [" + extractedTextEndpoint + "], error message [" + e.getMessage() + "]",
                OcrRequestException.ResultCode.FAIL_TO_SEND_RESULTS,
                CallTypeEnum.ASYNCHRONOUS,
                contextId,
                e);
        }
    }

    /**
     * Sends the extracted text with default values to the extracted text endpoint for non-retryable errors
     * @param   contextId               The CHS context ID 
     * @param   responseId              The Response ID of the request
     * @param   extractedTextEndpoint   The endpoint to send the extracted text to.
     * @param   errorResultCode         Enum for error code that is sent in the result_code back to the calling system
     * @param   totalProcessingTimeMs   Total Time in the request (milliseconds)
     */
    public void sendTextResultError(String contextId, String responseId, String extractedTextEndpoint, OcrRequestException.ResultCode errorResultCode, long totalProcessingTimeMs) {
        ExtractTextResultDto extractedTextError = ExtractTextResultDto
                .createErrorExtractTextResultDtoFromContextId(contextId, responseId, errorResultCode, totalProcessingTimeMs);

        LOG.infoContext(contextId, "Sending Error Message back to client", null);

        try {
            HttpEntity<ExtractTextResultDto> entity = new HttpEntity<>(extractedTextError);
            restTemplate.postForEntity(extractedTextEndpoint, entity, String.class);
        } catch (Exception e) {
            // Log the exception instead of re-throwing as we can not process this request further
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("extractedTextEndpoint", extractedTextEndpoint);
            LOG.errorContext(contextId, "Error sending Error Results for Image to Text Request", e, data);
        }
    }

}
