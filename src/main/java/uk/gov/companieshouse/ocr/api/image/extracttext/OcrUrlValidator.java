package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.Set;
import org.apache.commons.validator.ValidatorException;
import org.apache.commons.validator.routines.UrlValidator;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.SpringConfiguration;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OcrUrlValidator {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    private Set<String> whiteListHosts;

    @Autowired
    public OcrUrlValidator(SpringConfiguration springConfiguration) {

        this.whiteListHosts = Set.of(springConfiguration.getHostWhiteList().split(","));
        LOG.info(whiteListHosts.toString());

    }

    public OcrUrlValidator(String testWhiteListString) {
        this.whiteListHosts = Set.of(testWhiteListString.split(","));
    }

    //Can this method just be void?
    // ValidatorException may not be the correct one to use?
    //SHould a OcrRequestException here?
    public boolean isUrlValid(String url) throws ValidatorException {

        LOG.info(url);

        UrlValidator validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

        if(!validator.isValid(url)) {
            //bad URL
            throw new ValidatorException("Invalid URL");
        }

        if(!isUrlOnWhiteList(url)) {
            // URL not on white list
            throw new ValidatorException("URL not on white list");
        }

        return true;
    }

    public boolean isUrlOnWhiteList(String urlString) {

        try{
            // Create URL obj to extract host via method
            // Catch prompted be URL class(will throw if URL invalid)
            // In theory exception should not throw coz URL has been validated already by now
            URL url = new URL(urlString);

            if(whiteListHosts.contains(url.getHost())){
                LOG.info("Url host on list");
                return true;
            }
            LOG.info("Url host not on list");
            return false;

        } catch(MalformedURLException e) {
            LOG.error("URL object could not be created");
        }
        return false;
    }
}
