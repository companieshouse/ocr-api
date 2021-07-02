package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import static uk.gov.companieshouse.ocr.api.TestObjectMother.MOCK_TIFF_CONTENT;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.IMAGE_ENDPOINT;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.CONTEXT_ID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
class ImageRestClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ImageRestClient imageRestClient;

    @Test
    void getTiffImageSuccessfully() throws OcrRequestException {
        // given
        byte[] expected = MOCK_TIFF_CONTENT;
        when(restTemplate.getForEntity(IMAGE_ENDPOINT, byte[].class))
                .thenReturn(new ResponseEntity<>(MOCK_TIFF_CONTENT, HttpStatus.OK));

        // when
        byte[] actual = imageRestClient.getImageContentsFromEndpoint(CONTEXT_ID, IMAGE_ENDPOINT);

        // then
        assertThat(actual, is(expected));
    }

    @Test
    void canNotGetImage() {

        // given
        when(restTemplate.getForEntity(IMAGE_ENDPOINT, byte[].class))
                .thenThrow(RestClientException.class);

        var ocrRequestAssertion = assertThrows(OcrRequestException.class, () ->
            imageRestClient.getImageContentsFromEndpoint(CONTEXT_ID, IMAGE_ENDPOINT));

        assertEquals(1, ocrRequestAssertion.getResultCode().getCode());

    }
}
