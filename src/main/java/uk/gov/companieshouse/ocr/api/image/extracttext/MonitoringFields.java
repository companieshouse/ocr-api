package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.gov.companieshouse.ocr.api.common.CallTypeEnum;
import uk.gov.companieshouse.ocr.api.common.JsonLogFieldNameEnum;
import uk.gov.companieshouse.ocr.api.common.LogRecordEnum;

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

    // to add messageType, resultCode, mode (Async or Sync from enum)

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

    public Map<String, Object> toMap() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put(LogRecordEnum.MONITORING_FIELDS.getFieldName(), LogRecordEnum.MONITORING_FIELDS.getFieldValue());

        map.put(JsonLogFieldNameEnum.AVERAGE_CONFIDENCE_SCORE.getFieldName(), averageConfidenceScore);
        map.put(callType.getFieldName(), callType.getFieldValue());
        map.put(JsonLogFieldNameEnum.FILE_SIZE.getFieldName(), fileSize);
        map.put(JsonLogFieldNameEnum.LOWEST_CONFIDENCE_SCORE.getFieldName(), lowestConfidenceScore);
        map.put(JsonLogFieldNameEnum.OCR_PROCESSING_TIME_MS.getFieldName(), ocrProcessingTimeMs);
        map.put(JsonLogFieldNameEnum.RESULT_CODE.getFieldName(), resultCode);
        map.put(JsonLogFieldNameEnum.TOTAL_PROCESSING_TIME_MS.getFieldName(), totalProcessingTimeMs);
        map.put(JsonLogFieldNameEnum.TIME_ON_EXECUTER_QUEUE_MS.getFieldName(), timeOnExecuterQueue);
        map.put(JsonLogFieldNameEnum.TOTAL_PAGES.getFieldName(), totalPages);

        return  map;        
    }

    
    
}
