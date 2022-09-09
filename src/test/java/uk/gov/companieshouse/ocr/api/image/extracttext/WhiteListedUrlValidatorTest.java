package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.ocr.api.groups.TestType;
import uk.gov.companieshouse.ocr.api.urlvalidator.UrlValidatorException;
import uk.gov.companieshouse.ocr.api.urlvalidator.WhiteListedUrlValidator;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
class WhiteListedUrlValidatorTest {

    private WhiteListedUrlValidator whiteListedUrlValidator;

    @BeforeEach
    void setupTests() {

        this.whiteListedUrlValidator = new WhiteListedUrlValidator("localhost,chips.local,amazon,testurl.com,chpdev-sl7,chpdev-sl6,chpdev-pl7,chpdev-pl6,chpdev-sl7.internal.ch");
    }

    @Test
    void testUrlIsValidAndOnWhiteList1() throws UrlValidatorException {

        whiteListedUrlValidator.validateUrl("http://chpdev-sl7:36011/chips");
    }

    @Test
    void testUrlIsValidAndOnWhiteList2() throws UrlValidatorException {

        whiteListedUrlValidator.validateUrl("https://testurl.com/test");
    }

    @Test
    void testUrlIsValidAndOnWhiteList3() throws UrlValidatorException {

        whiteListedUrlValidator.validateUrl("http://chpdev-sl7.internal.ch:36011/chips");
    }

    @Test()
    void testUrlIsValidAndNotOnWhiteList() {

        String url = "http://google.com";
        UrlValidatorException thrown = assertThrows(UrlValidatorException.class, () -> {

            whiteListedUrlValidator.validateUrl(url);
        });

        assertEquals("URL not on white list: " + url, thrown.getMessage());
    }

    @Test
    void testUrlIsNotValid() {

        String url = "google.com";
        UrlValidatorException thrown = assertThrows(UrlValidatorException.class, () -> {

            whiteListedUrlValidator.validateUrl(url);
        });

        assertEquals("Invalid URL: " + url, thrown.getMessage());
    }
}
