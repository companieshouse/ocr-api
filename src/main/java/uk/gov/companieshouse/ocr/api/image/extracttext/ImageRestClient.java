package uk.gov.companieshouse.ocr.api.image.extracttext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;


@Component
public class ImageRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    
    @Autowired
    private RestTemplate restTemplate;

    /**
     * Gets a byte array representation of an image from the endpoint passed through.
     * @param   imageEndpoint   The endpoint that the image is retrieved from.
     * @return  A byte array of image contents used for the OCR text extraction.
     */
    public byte[] getImageContentsFromEndpoint(String contextId, String imageEndpoint) throws OcrRequestException{
        
        LOG.debugContext(contextId, "Image from [" + imageEndpoint + "]",null);
        
        try {
            return restTemplate.getForEntity(imageEndpoint, byte[].class).getBody();

        } catch (Exception e) {
             throw new OcrRequestException(
                 "Fail to get Image file from requesting system url [" + imageEndpoint + "], error message [" + e.getMessage() + "]",
                 OcrRequestException.ResultCode.FAIL_TO_GET_IMAGE,
                 CallTypeEnum.ASYNCHRONOUS,
                 contextId,
                 e);
        }
    }
}
