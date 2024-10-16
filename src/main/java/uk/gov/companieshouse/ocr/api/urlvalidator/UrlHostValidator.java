package uk.gov.companieshouse.ocr.api.urlvalidator;

import java.util.Set;
import org.apache.commons.validator.routines.UrlValidator;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Validates a String as a URL by asserting that it parses as a well-formed URL and that the host component of the URL is on a configured whitelist of allowed hosts.
 * If the the URL is invalid or the host is not present on the host list an exception will be thrown.
 */

public class UrlHostValidator {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    private final Set<String> hostList;

    public UrlHostValidator(String hostListString) {
        this.hostList = Set.of(hostListString.split(","));
        LOG.debug("White Listed Hosts: " + hostList.toString());
    }

    public void validateUrl(String url) throws UrlValidatorException {

        LOG.debug("Validating URL: " + url);

        var validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

        if(!validator.isValid(url)) {
            throw new UrlValidatorException("Invalid URL: " + url);
        }

        if(!isUrlOnHostList(url)) {
            throw new UrlValidatorException("URL not on white list: " + url);
        }

    }

    private boolean isUrlOnHostList(String urlString) throws UrlValidatorException {

        try{

            var url = new URL(urlString);
            if(hostList.contains(url.getHost())){
                LOG.debug("Url host on list: " + url.getHost());
                return true;
            }

            LOG.debug("Url host not on list: " + url.getHost());
            return false;

        } catch(MalformedURLException e) {
            throw new UrlValidatorException("URL object could not be created: " + e.getMessage());
        }
    }
}
