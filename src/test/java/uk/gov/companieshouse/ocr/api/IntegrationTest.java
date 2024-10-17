package uk.gov.companieshouse.ocr.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import net.sourceforge.tess4j.TesseractException;
import uk.gov.companieshouse.ocr.api.groups.TestType;
import uk.gov.companieshouse.ocr.api.image.extracttext.ExtractTextResultDto;
import uk.gov.companieshouse.ocr.api.image.extracttext.SyncImageOcrApiController;

@Tag(TestType.INTEGRATION)
class IntegrationTest {

    private static final Path RESOURCES_PATH = Paths.get("src", "test", "resources");
    private static final String SAMPLE_TIFF = "sample-articles-of-association.tif";
    private static final String TEST_RESPONSE_ID = "test-response-id";

    private String apiEndpoint = "ocr-api"; // no Spring context so hard code it

    @Test
    void verifySuccessfulTextExtractFromTesseract() throws IOException, TesseractException {

        var image = loadFile(SAMPLE_TIFF);
        var result = extractText(image);

        writeTextFile(SAMPLE_TIFF, result.getExtractedText());

        assertEquals(90, result.getAverageConfidenceScore());
        assertEquals(63, result.getLowestConfidenceScore());
        assertTrue(result.getTotalProcessingTimeMs() > 0L);
        assertEquals(TEST_RESPONSE_ID, result.getResponseId());
        assertThat(result.getExtractedText(), containsString("SAMPLE LTD"));
    }

    private FileSystemResource loadFile(String fileName) throws IOException {
        var filePath = Paths.get(RESOURCES_PATH.toString(), fileName);
        return new FileSystemResource(filePath);
    }

    private ExtractTextResultDto extractText(FileSystemResource fileBytes) {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", fileBytes);
        body.add("responseId", TEST_RESPONSE_ID);

        var requestEntity = new HttpEntity<>(body, headers);

        var url = System.getenv("OCR_TESSERACT_POC_URL");
        if (StringUtils.isEmpty(url)) {
            url = "http://localhost:8080/";
        }

        url += apiEndpoint + SyncImageOcrApiController.TIFF_EXTRACT_TEXT_PARTIAL_URL;
        System.out.println("Using API URL [" + url + "]");

        var restTemplate = new RestTemplate();
        var response = restTemplate.postForEntity(url, requestEntity, ExtractTextResultDto.class);

        return response.getBody();
    }

    private void writeTextFile(String tifFilename,  String text) throws IOException {

        var indexDot = tifFilename.indexOf(".tif");

        var textFilename = tifFilename.substring(0, indexDot) + ".txt";

        var txtPath = Paths.get(RESOURCES_PATH.toString(), textFilename);

        Files.write(txtPath, text.getBytes());
    }

}