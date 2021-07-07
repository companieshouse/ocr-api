package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
class MonitoringFieldsTest {

    private static final long TIME_ON_EXECUTOR_QUEUE = 500;
    private static final int AVERAGE_CONFIDENCE_SCORE = 60;
    private static final int LOWEST_CONFIDENCE_SCORE = 5;
    private static final long OCR_PROCESSING_TIME_MS = 8321;
    private static final long TOTAL_PROCESSING_TIME_MS = 8842;
    private static final int FILE_SIZE = 12345;
    private static final int RESULT_CODE = 99;

    @Test
    void objectConstructed() {
        
        var textConversionResult = new TextConversionResult("context-id", "response-id", TIME_ON_EXECUTOR_QUEUE, FILE_SIZE);
        textConversionResult.addPage();
        textConversionResult.addPage();

        var extractTextResult = new ExtractTextResultDto();
        extractTextResult.setAverageConfidenceScore(AVERAGE_CONFIDENCE_SCORE);
        extractTextResult.setLowestConfidenceScore(LOWEST_CONFIDENCE_SCORE);
        extractTextResult.setOcrProcessingTimeMs(OCR_PROCESSING_TIME_MS);
        extractTextResult.setTotalProcessingTimeMs(TOTAL_PROCESSING_TIME_MS);
        extractTextResult.setResultCode(RESULT_CODE);

        var monitoringFields = new MonitoringFields(textConversionResult, extractTextResult, CallTypeEnum.ASYNCHRONOUS);

        var actualMap = monitoringFields.toMap();

        Map<String, Object> expectedMap = new LinkedHashMap<>(); 
  
        expectedMap.put("logRecordName", "ocrMonitoring");
        expectedMap.put("averageConfidenceScore", AVERAGE_CONFIDENCE_SCORE);
        expectedMap.put("callType", "asynchronous");
        expectedMap.put("fileSize", FILE_SIZE);
        expectedMap.put("lowestConfidenceScore", LOWEST_CONFIDENCE_SCORE);
        expectedMap.put("ocrProcessingTimeMs", OCR_PROCESSING_TIME_MS);
        expectedMap.put("resultCode", RESULT_CODE);
        expectedMap.put("timeOnExecuterQueueMs", TIME_ON_EXECUTOR_QUEUE);
        expectedMap.put("totalPages", 2);
        expectedMap.put("totalProcessingTimeMs", TOTAL_PROCESSING_TIME_MS);

        assertThat(actualMap.size(), is(10));

        assertThat(actualMap, is(expectedMap));
    }

}