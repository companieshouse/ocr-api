package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.validator.ValidatorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
public class OcrUrlValidatorTest {

    private OcrUrlValidator ocrUrlValidator;

    @BeforeEach
    void setupTests() {

        // Struggling to create ocrUrlValidator in this class, was always null
        // Added a second constructor in the class to use here
        // Can't imagine that is the best this to do though
        this.ocrUrlValidator = new OcrUrlValidator("localhost,chips.local,amazon");
    }

    @Test
    void testUrlIsValidAndOnWhiteList() throws Throwable {

        // when
        boolean result = ocrUrlValidator.isUrlValid("http://localhost:8080/ocr-api");

        assertTrue(result);
    }


    @Test()
    void testUrlIsValidAndNotOnWhiteList() throws Throwable {

        // when
//        boolean result = ocrUrlValidator.isUrlValid("http://google.com");

        ValidatorException thrown = assertThrows(ValidatorException.class, () -> {
            //Code under test

            ocrUrlValidator.isUrlValid("http://google.com");
        });

        assertEquals("URL not on white list", thrown.getMessage());
//        assertFalse(result);
    }

    @Test
    void testUrlIsNotValid() throws Throwable {

        ValidatorException thrown = assertThrows(ValidatorException.class, () -> {

            ocrUrlValidator.isUrlValid("google.com");
        });

        assertEquals("Invalid URL", thrown.getMessage());
    }
}
