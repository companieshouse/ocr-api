package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.common.JsonConstants;
import uk.gov.companieshouse.ocr.api.image.extracttext.OcrRequestException.ResultCode;

/**
 *  This is a DTO for data to be logged ONCE per OCR transaction and then used in Log processing to produce Dashboards etc
 */
public class MonitoringFields {

    private final int averageConfidenceScore;

    private final int lowestConfidenceScore;

    private final long ocrProcessingTimeMs;

    private final long totalProcessingTimeMs;   

    private final long timeOnExecuterQueue;

    private final Integer totalPages;

    private final int fileSize;

    private final CallTypeEnum callType;

    private final int resultCode;


    public MonitoringFields(TextConversionResult textConversionResult, ExtractTextResultDto extractTextResultDto, CallTypeEnum callType) {

        this.averageConfidenceScore = extractTextResultDto.getAverageConfidenceScore();
        this.lowestConfidenceScore = extractTextResultDto.getLowestConfidenceScore();
        this.ocrProcessingTimeMs = extractTextResultDto.getOcrProcessingTimeMs();
        this.totalProcessingTimeMs = extractTextResultDto.getTotalProcessingTimeMs();
        this.timeOnExecuterQueue = textConversionResult.getTimeOnExecuterQueue();
        this.resultCode = extractTextResultDto.getResultCode();
        this.totalPages = textConversionResult.getTotalPages();
        this.fileSize = textConversionResult.getFileSize();
        this.callType = callType;
    }

    public MonitoringFields(long totalProcessingTimeMs, ResultCode resultCode, CallTypeEnum callType, int fileSize) {
        this.averageConfidenceScore = 0;
        this.lowestConfidenceScore = 0;
        this.ocrProcessingTimeMs = 0;
        this.totalProcessingTimeMs = totalProcessingTimeMs;
        this.timeOnExecuterQueue = 0;
        this.resultCode = resultCode.getCode();
        this.totalPages = 0;
        this.fileSize = fileSize;
        this.callType = callType;
	}


    public Map<String, Object> toMap() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.MONITORING_LOG_RECORD);

        map.put(JsonConstants.AVERAGE_CONFIDENCE_SCORE_NAME, averageConfidenceScore);
        map.put(JsonConstants.CALL_TYPE_NAME, callType.getFieldValue());
        map.put(JsonConstants.FILE_SIZE_NAME, fileSize);
        map.put(JsonConstants.LOWEST_CONFIDENCE_SCORE_NAME, lowestConfidenceScore);
        map.put(JsonConstants.OCR_PROCESSING_TIME_MS_NAME, ocrProcessingTimeMs);
        map.put(JsonConstants.RESULT_CODE_NAME, resultCode);
        map.put(JsonConstants.TOTAL_PROCESSING_TIME_MS_NAME, totalProcessingTimeMs);
        map.put(JsonConstants.TIME_ON_QUEUE_MS_NAME, timeOnExecuterQueue);
        map.put(JsonConstants.TOTAL_PAGES_NAME, totalPages);

        return  map;        
    }
    
}
