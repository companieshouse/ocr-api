package uk.gov.companieshouse.ocr.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import uk.gov.companieshouse.ocr.api.image.extracttext.ExtractTextResultDTO;

@Tag("integration-test")
public class IntegrationTest {

    private static final Path RESOURCES_PATH = Paths.get("src", "test", "resources");
    private static final String SAMPLE_TIFF = "sample-articles-of-association.tif";

    @Test
    public void verifySuccessfulTextExtractFromTesseract() throws IOException, TesseractException {

        var image = loadFile(SAMPLE_TIFF);
        var result = extractText(image);

        writeTextFile(SAMPLE_TIFF, result.getExtractedText());

        assertEquals(90, result.getAverageConfidenceScore());
        assertEquals(68, result.getLowestConfidenceScore());
        assertNotNull(result.getProcessingTimeMs());
        assertThat(result.getExtractedText(), containsString("SAMPLE LTD"));
    }

    private FileSystemResource loadFile(String fileName) throws IOException {
        var filePath = Paths.get(RESOURCES_PATH.toString(), fileName);
        return new FileSystemResource(filePath);
    }

    private ExtractTextResultDTO extractText(FileSystemResource fileBytes) {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", fileBytes);
        body.add("externalReferenceId","test-ref-id");

        var requestEntity = new HttpEntity<>(body, headers);

        var url = System.getenv("OCR_TESSERACT_POC_URL");
        if (StringUtils.isEmpty(url)) {
            url = "http://localhost:8080";
        }

        url += "/api/ocr/image/extractText";
        System.out.println("Using API URL [" + url + "]");

        var restTemplate = new RestTemplate();
        var response = restTemplate.postForEntity(url, requestEntity, ExtractTextResultDTO.class);

        return response.getBody();
    }

    private void writeTextFile(String tifFilename,  String text) throws IOException {

        var indexDot = tifFilename.indexOf(".tif");

        var textFilename = tifFilename.substring(0, indexDot) + ".txt";

        var txtPath = Paths.get(RESOURCES_PATH.toString(), textFilename);

        Files.write(txtPath, text.getBytes());
    }


}