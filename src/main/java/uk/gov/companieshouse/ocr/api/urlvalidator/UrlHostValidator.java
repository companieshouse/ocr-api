package uk.gov.companieshouse.ocr.api.urlvalidator;

import java.util.Set;
import org.apache.commons.validator.routines.UrlValidator;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class is used to check whether a URL host is present on a host list set by an application env var.
 * The host list is a comma separated list that must contain NO white space.
 * When the public method is called it will verify the the URL passed is valid.
 * If it is then the URL host is extracted and compared against the host list.
 * If the host is not present or the URL is invalid and exception will be thrown.
 */

public class WhiteListedUrlValidator {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    private final Set<String> hostList;

    public WhiteListedUrlValidator(String hostListString) {
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
