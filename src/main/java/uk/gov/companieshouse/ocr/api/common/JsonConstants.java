package uk.gov.companieshouse.ocr.api.common;

public final class JsonConstants {

    // Field Names (used in POJO Json definitions and in Log field names)
    public static final String ACTIVE_POOL_SIZE_NAME = "active_pool_size";
    public static final String AVERAGE_CONFIDENCE_SCORE_NAME ="average_confidence_score";
    public static final String CALL_TYPE_NAME = "call_type";
    public static final String CONTEXT_ID = "context_id";
    public static final String CLIENT_IP_NAME = "client_ip";
    public static final String FILE_CONTENT_TYPE_NAME = "file_content_type";
    public static final String FILE_SIZE_NAME = "file_size";
    public static final String HTTP_REFERER_NAME = "http_referer";
    public static final String INSTANCE_UUID_NAME = "instance_uuid";
    public static final String LOWEST_CONFIDENCE_SCORE_NAME = "lowest_confidence_score";
    public static final String OCR_PROCESSING_TIME_MS_NAME = "ocr_processing_time_ms";
    public static final String LARGEST_POOL_SIZE_NAME = "largest_pool_size";
    public static final String MAX_POOL_SIZE_NAME = "max_pool_size";
    public static final String ORIGINAL_FILENAME_NAME = "original_filename";
    public static final String POOL_SIZE_NAME = "pool_size";
    public static final String PROCESSORS_NAME = "processors_size";
    public static final String QUEUE_CAPACITY_NAME = "queue_capacity";
    public static final String QUEUE_SIZE_NAME = "queue_size";
    public static final String RESPONSE_ID = "response_id";
    public static final String RESULT_CODE_NAME = "result_code";
    public static final String THREAD_NAME_NAME = "thread_name";
    public static final String THREAD_NAME_PREFIX_NAME = "thread_name_prefix";
    public static final String TIME_ON_QUEUE_MS_NAME = "time_on_queue_ms";
    public static final String TOTAL_PAGES_NAME = "total_pages";
    public static final String TOTAL_PROCESSING_TIME_MS_NAME = "total_processing_time_ms";

    // Log Record Names
    public static final String LOG_RECORD_NAME = "log_record_name";

    public static final String HTTP_REQUEST_LOG_RECORD = "http_request";
    public static final String MONITORING_LOG_RECORD = "ocr_monitoring";
    public static final String STATISTICS_LOG_RECORD = "ocr_statistics";
    public static final String THREAD_POOL_EXECUTOR_CONFIG_LOG_RECORD = "thread_pool_executor_config";
    public static final String POST_QUEUE_LOG_RECORD = "post_queue";

    private JsonConstants(){}
    
}
