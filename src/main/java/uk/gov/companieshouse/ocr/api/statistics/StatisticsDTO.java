package uk.gov.companieshouse.ocr.api.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsDTO {

    @JsonProperty("queue_size")
    int queueSize;

    @JsonProperty("instance_uuid")
    String instanceUuid;

    @JsonProperty("tesseract_thread_pool_size")
    int tesseractThreadPoolSize;

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    } 

    public int getTesseractThreadPoolSize() {
        return tesseractThreadPoolSize;
    }

    public void setTesseractThreadPoolSize(int tesseractThreadPoolSize) {
        this.tesseractThreadPoolSize = tesseractThreadPoolSize;
    }

}
