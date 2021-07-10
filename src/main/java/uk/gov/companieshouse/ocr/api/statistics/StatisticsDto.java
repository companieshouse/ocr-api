package uk.gov.companieshouse.ocr.api.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.companieshouse.ocr.api.common.JsonConstants;

public class StatisticsDto {

    @JsonProperty(JsonConstants.INSTANCE_UUID_NAME)
    String instanceUuid;

    @JsonProperty(JsonConstants.ACTIVE_POOL_SIZE_NAME)
    int activePoolSize;

    @JsonProperty(JsonConstants.LARGEST_POOL_SIZE_NAME)
    int largestPoolSize;

    @JsonProperty(JsonConstants.POOL_SIZE_NAME)
    int poolSize;

    @JsonProperty(JsonConstants.QUEUE_SIZE_NAME)
    int queueSize;

    /*  ------ Accessors ------- */ 

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public int getActivePoolSize() {
        return activePoolSize;
    }

    public void setActivePoolSize(int activePoolSize) {
        this.activePoolSize = activePoolSize;
    }

    public int getLargestPoolSize() {
        return largestPoolSize;
    }

    public void setLargestPoolSize(int largestPoolSize) {
        this.largestPoolSize = largestPoolSize;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public Map<String, Object> toMap() {

        // tesseractQueueSize, tesseractPoolSize, tesseractActivePoolSize (same for ocrRequest but queue should always be zero)

        Map<String, Object> map = new LinkedHashMap<>();

        map.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.STATISTICS_LOG_RECORD);
        map.put(JsonConstants.INSTANCE_UUID_NAME, instanceUuid);
        map.put(JsonConstants.QUEUE_SIZE_NAME, queueSize);
        map.put(JsonConstants.ACTIVE_POOL_SIZE_NAME, activePoolSize);
        map.put(JsonConstants.POOL_SIZE_NAME, poolSize);
        map.put(JsonConstants.LARGEST_POOL_SIZE_NAME, largestPoolSize);

        return  map;        
    }

}
