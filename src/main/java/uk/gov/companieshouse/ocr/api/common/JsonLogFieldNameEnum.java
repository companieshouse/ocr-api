package uk.gov.companieshouse.ocr.api.common;

/**
 * This contains field names that are used in hash maps.
 * 
 * If these field names are changed then this effect the logging anlytics in CloudWatch
 */
public enum JsonLogFieldNameEnum implements LogFieldName{

    AVERAGE_CONFIDENCE_SCORE("averageConfidenceScore"),
    FILE_SIZE("fileSize"),
    LOWEST_CONFIDENCE_SCORE("lowestConfidenceScore"),
    OCR_PROCESSING_TIME_MS("ocrProcessingTimeMs"),
    RESULT_CODE("resultCode"),
    TIME_ON_EXECUTER_QUEUE_MS("timeOnExecuterQueueMs"),
    TOTAL_PAGES("totalPages"),
    TOTAL_PROCESSING_TIME_MS("totalProcessingTimeMs");

    private final String fieldName;

    private JsonLogFieldNameEnum(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}