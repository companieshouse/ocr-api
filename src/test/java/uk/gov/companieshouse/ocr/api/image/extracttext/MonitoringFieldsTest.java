package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
class MonitoringFieldsTest {

	private static final long TIME_ON_EXECUTOR_QUEUE = 500;
	private static final int AVERAGE_CONFIDENCE_SCORE = 60;
	private static final int LOWEST_CONFIDENCE_SCORE = 5;
	private static final long OCR_PROCESSING_TIME_MS = 8321;
	private static final long OCR_TOTAL_PROCESSING_TIME_MS = 8842;

	@Test
	void objectConstructed() {
		
		var textConversionResult = new TextConversionResult("context-id", "response-id", TIME_ON_EXECUTOR_QUEUE);
        textConversionResult.addPage();
		textConversionResult.addPage();

		var extractTextResult = new ExtractTextResultDto();
		extractTextResult.setAverageConfidenceScore(AVERAGE_CONFIDENCE_SCORE);
		extractTextResult.setLowestConfidenceScore(LOWEST_CONFIDENCE_SCORE);
		extractTextResult.setOcrProcessingTimeMs(OCR_PROCESSING_TIME_MS);
		extractTextResult.setTotalProcessingTimeMs(OCR_TOTAL_PROCESSING_TIME_MS);

		var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult);

		assertEquals(TIME_ON_EXECUTOR_QUEUE, monitoringFields.getTimeOnExecuterQueue());
		assertEquals(AVERAGE_CONFIDENCE_SCORE, monitoringFields.getAverageConfidenceScore());
		assertEquals(LOWEST_CONFIDENCE_SCORE, monitoringFields.getLowestConfidenceScore());
		assertEquals(OCR_PROCESSING_TIME_MS, monitoringFields.getOcrProcessingTimeMs());
		assertEquals(OCR_TOTAL_PROCESSING_TIME_MS, monitoringFields.getTotalProcessingTimeMs());
		assertEquals(2, monitoringFields.getTotalPages());

	}

}