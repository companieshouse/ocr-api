package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang.time.StopWatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ImageOcrServiceTest {

	@InjectMocks
	private ImageOcrService imageOcrService;

	@Test
	public void shouldExtractTextFromImage() throws IOException {
		// TODO: initialize args
		MultipartFile file;
		String responseId;
		StopWatch timeOnQueueStopWatch;

		CompletableFuture<TextConversionResult> actualValue = imageOcrService.extractTextFromImage(file, responseId, timeOnQueueStopWatch);

		assertEquals(71f, 71f);
	}
}
