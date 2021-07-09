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
    private static final int RESULT_CODE = 999;

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
  
        expectedMap.put("log_record_name", "ocr_monitoring");
        expectedMap.put("average_confidence_score", AVERAGE_CONFIDENCE_SCORE);
        expectedMap.put("call_type", "asynchronous");
        expectedMap.put("file_size", FILE_SIZE);
        expectedMap.put("lowest_confidence_score", LOWEST_CONFIDENCE_SCORE);
        expectedMap.put("ocr_processing_time_ms", OCR_PROCESSING_TIME_MS);
        expectedMap.put("result_code", RESULT_CODE);
        expectedMap.put("time_on_executer_queue_ms", TIME_ON_EXECUTOR_QUEUE);
        expectedMap.put("total_pages", 2);
        expectedMap.put("total_processing_time_ms", TOTAL_PROCESSING_TIME_MS);

        assertThat(actualMap.size(), is(10));

        assertThat(actualMap, is(expectedMap));
    }

}