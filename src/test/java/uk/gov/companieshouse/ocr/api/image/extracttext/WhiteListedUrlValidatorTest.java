package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.validator.ValidatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.companieshouse.ocr.api.groups.TestType;
import uk.gov.companieshouse.ocr.api.whiteListedUrlValidator.OcrUrlValidator;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
public class OcrUrlValidatorTest {

    private OcrUrlValidator ocrUrlValidator;

    @BeforeEach
    void setupTests() {

        // Struggling to create ocrUrlValidator in this class, was always null
        // Added a second constructor in the class to use here
        // Can't imagine that is the best this to do though
        this.ocrUrlValidator = new OcrUrlValidator("localhost,chips.local,amazon,testurl.com,chpdev-sl7,chpdev-sl6,chpdev-pl7,chpdev-pl6");
    }

    @Test
    void testUrlIsValidAndOnWhiteList() throws ValidatorException {

        ocrUrlValidator.validateUrl("http://chpdev-sl7:36011/chips/cff");
    }

    @Test()
    void testUrlIsValidAndNotOnWhiteList() {
        String url = "http://google.com";

        ValidatorException thrown = assertThrows(ValidatorException.class, () -> {

            ocrUrlValidator.validateUrl(url);
        });

        assertEquals("URL not on white list: " + url, thrown.getMessage());
    }

    @Test
    void testUrlIsNotValid() {

        String url = "google.com";

        ValidatorException thrown = assertThrows(ValidatorException.class, () -> {

            ocrUrlValidator.validateUrl(url);
        });

        assertEquals("Invalid URL: " + url, thrown.getMessage());
    }
}
