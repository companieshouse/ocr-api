package uk.gov.companieshouse.ocr.api.urlValidator;

import java.util.Set;
import org.apache.commons.validator.routines.UrlValidator;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;


public class WhiteListedUrlValidator {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    private final Set<String> whiteListHosts;

    public WhiteListedUrlValidator(String whiteListString) {
        this.whiteListHosts = Set.of(whiteListString.split(","));
        LOG.debug("White Listed Hosts: " + whiteListHosts.toString());
    }

    public void validateUrl(String url) throws UrlValidatorException {

        LOG.debug("Validating URL: " + url);

        UrlValidator validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

        if(!validator.isValid(url)) {
            throw new UrlValidatorException("Invalid URL: " + url);
        }

        if(!isUrlOnWhiteList(url)) {
            throw new UrlValidatorException("URL not on white list: " + url);
        }

    }

    public boolean isUrlOnWhiteList(String urlString) throws UrlValidatorException {

        try{

            URL url = new URL(urlString);
            if(whiteListHosts.contains(url.getHost())){
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
