package uk.gov.companieshouse.ocr.api.image.extracttext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;

@Component
public class CallbackExtractedTextRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    
    @Autowired
    private RestTemplate restTemplate;

    
    /**
     * Sends the extracted text to the extracted text endpoint.
     * @param   extractedTextEndpoint   The endpoint to send the extracted text to.
     * @param   extractedText           The extracted text DTO object.
     * @throws OcrRequestException
     */
    public void sendTextResult(String extractedTextEndpoint, ExtractTextResultDto extractedText) throws OcrRequestException {
        
        try {
            HttpEntity<ExtractTextResultDto> entity = new HttpEntity<>(extractedText);
            restTemplate.postForEntity(extractedTextEndpoint, entity, String.class);

        } catch (Exception e) {
            throw new OcrRequestException(
                "Fail to send results back to calling application at url [" + extractedTextEndpoint + "], error message [" + e.getMessage() + "]",
                OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE,
                e);
        }
    }
    
    
}
