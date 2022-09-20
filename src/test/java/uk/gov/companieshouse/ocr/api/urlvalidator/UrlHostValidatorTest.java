package uk.gov.companieshouse.ocr.api.urlvalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
class UrlHostValidatorTest {

    private UrlHostValidator urlHostValidator;

    @BeforeEach
    void setupTests() {

        this.urlHostValidator = new UrlHostValidator("localhost,chips.local,testurl.com,chpdev-sl7,chpdev-sl6,chpdev-pl7,chpdev-pl6,chpdev-sl7.internal.ch");
    }

    @Test
    void testUrlIsValidAndOnWhiteList1() throws UrlValidatorException {

        urlHostValidator.validateUrl("http://chpdev-sl7:36011/chips");
    }

    @Test
    void testUrlIsValidAndOnWhiteList2() throws UrlValidatorException {

        urlHostValidator.validateUrl("https://testurl.com/test");
    }

    @Test
    void testUrlIsValidAndOnWhiteList3() throws UrlValidatorException {

        urlHostValidator.validateUrl("http://chpdev-sl7.internal.ch:36011/chips");
    }

    @Test()
    void testUrlIsValidAndNotOnWhiteList() {

        String url = "http://google.com";
        UrlValidatorException thrown = assertThrows(UrlValidatorException.class, () -> {

            urlHostValidator.validateUrl(url);
        });

        assertEquals("URL not on white list: " + url, thrown.getMessage());
    }

    @Test
    void testUrlIsNotValid() {

        String url = "google.com";
        UrlValidatorException thrown = assertThrows(UrlValidatorException.class, () -> {

            urlHostValidator.validateUrl(url);
        });

        assertEquals("Invalid URL: " + url, thrown.getMessage());
    }
}
