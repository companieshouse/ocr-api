package uk.gov.companieshouse.ocr.api.common;

public final class JsonConstants {

    // Field Names (used in POJO Json definitions and in Log field names)
    public static final String AVERAGE_CONFIDENCE_SCORE_NAME ="average_confidence_score";
    public static final String CALL_TYPE_NAME = "call_type";
    public static final String FILE_SIZE_NAME = "file_size";
    public static final String INSTANCE_UUID_NAME = "instance_uuid";
    public static final String LOWEST_CONFIDENCE_SCORE_NAME = "lowest_confidence_score";
    public static final String OCR_PROCESSING_TIME_MS_NAME = "ocr_processing_time_ms";
    public static final String OCR_REQUEST_ACTIVE_POOL_SIZE_NAME = "ocr_request_active_pool_size";
    public static final String OCR_REQUEST_LARGEST_POOL_SIZE_NAME = "ocr_request_largest_pool_size";
    public static final String OCR_REQUEST_POOL_SIZE_NAME = "ocr_request_pool_size";
    public static final String OCR_REQUEST_QUEUE_SIZE_NAME = "ocr_request_queue_size";
    public static final String RESULT_CODE_NAME = "result_code";
    public static final String TESSERACT_ACTIVE_POOL_SIZE_NAME = "tesseract_active_pool_size";
    public static final String TESSERACT_LARGEST_POOL_SIZE_NAME = "tesseract_largest_pool_size";
    public static final String TESSERACT_POOL_SIZE_NAME = "tesseract_pool_size";
    public static final String TESSERACT_QUEUE_SIZE_NAME = "tesseract_queue_size";
    public static final String TIME_ON_EXECUTER_QUEUE_MS_NAME = "time_on_executer_queue_ms";
    public static final String TOTAL_PAGES_NAME = "total_pages";
    public static final String TOTAL_PROCESSING_TIME_MS_NAME = "total_processing_time_ms";

    // Log Record Names
    public static final String LOG_RECORD_NAME = "log_record_name";

    public static final String MONITORING_LOG_RECORD = "ocr_monitoring";
    public static final String STATISTICS_LOG_RECORD = "ocr_statistics";

    private JsonConstants(){}
    
}