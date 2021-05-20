package uk.gov.companieshouse.ocr.api.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsDto {

    @JsonProperty("instance_uuid")
    String instanceUuid;

    @JsonProperty("queue_size")
    int queueSize;

    @JsonProperty("tesseract_thread_pool_size")
    int tesseractThreadPoolSize;

    /*  ------ Accessors ------- */

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    } 

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getTesseractThreadPoolSize() {
        return tesseractThreadPoolSize;
    }

    public void setTesseractThreadPoolSize(int tesseractThreadPoolSize) {
        this.tesseractThreadPoolSize = tesseractThreadPoolSize;
    }

    public Map<String, Object> toMap() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("instanceUuid", instanceUuid);
        map.put("queueSize", queueSize);
        map.put("tesseractThreadPoolSize", tesseractThreadPoolSize);

        return  map;        
    }

}
