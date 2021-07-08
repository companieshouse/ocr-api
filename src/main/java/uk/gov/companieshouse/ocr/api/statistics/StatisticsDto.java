package uk.gov.companieshouse.ocr.api.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.companieshouse.ocr.api.common.JsonConstants;

public class StatisticsDto {

    @JsonProperty(JsonConstants.INSTANCE_UUID_NAME)
    String instanceUuid;

    @JsonProperty(JsonConstants.TESSERACT_QUEUE_SIZE_NAME)
    int tesseractQueueSize;

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
        return tesseractQueueSize;
    }

    public void setQueueSize(int queueSize) {
        this.tesseractQueueSize = queueSize;
    }

    public int getTesseractThreadPoolSize() {
        return tesseractThreadPoolSize;
    }

    public void setTesseractThreadPoolSize(int tesseractThreadPoolSize) {
        this.tesseractThreadPoolSize = tesseractThreadPoolSize;
    }

    public Map<String, Object> toMap() {

        // tesseractQueueSize, tesseractPoolSize, tesseractActivePoolSize (same for ocrRequest but queue should always be zero)

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("instanceUuid", instanceUuid);
        map.put("queueSize", tesseractQueueSize);
        map.put("tesseractThreadPoolSize", tesseractThreadPoolSize);

        return  map;        
    }

}
