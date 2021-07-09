package uk.gov.companieshouse.ocr.api.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.companieshouse.ocr.api.common.JsonConstants;

public class StatisticsDto {

    @JsonProperty(JsonConstants.INSTANCE_UUID_NAME)
    String instanceUuid;

    @JsonProperty(JsonConstants.OCR_REQUEST_ACTIVE_POOL_SIZE_NAME)
    int ocrRequestActivePoolSize;

    @JsonProperty(JsonConstants.OCR_REQUEST_LARGEST_POOL_SIZE_NAME)
    int ocrRequestLargestPoolSize;

    @JsonProperty(JsonConstants.OCR_REQUEST_POOL_SIZE_NAME)
    int ocrRequestPoolSize;

    @JsonProperty(JsonConstants.OCR_REQUEST_QUEUE_SIZE_NAME)
    int ocrRequestQueueSize;

    @JsonProperty(JsonConstants.TESSERACT_ACTIVE_POOL_SIZE_NAME)
    int tesseractActivePoolSize;

    @JsonProperty(JsonConstants.TESSERACT_LARGEST_POOL_SIZE_NAME)
    int tesseractLargestPoolSize;

    @JsonProperty(JsonConstants.TESSERACT_POOL_SIZE_NAME)
    int tesseractPoolSize;

    @JsonProperty(JsonConstants.TESSERACT_QUEUE_SIZE_NAME)
    int tesseractQueueSize;


    /*  ------ Accessors ------- */

    public int getTesseractQueueSize() {
        return tesseractQueueSize;
    }

    public void setTesseractQueueSize(int tesseractQueueSize) {
        this.tesseractQueueSize = tesseractQueueSize;
    }

    public int getTesseractActivePoolSize() {
        return tesseractActivePoolSize;
    }

    public void setTesseractActivePoolSize(int tesseractActivePoolSize) {
        this.tesseractActivePoolSize = tesseractActivePoolSize;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    } 

    public int getOcrRequestActivePoolSize() {
        return ocrRequestActivePoolSize;
    }

    public void setOcrRequestActivePoolSize(int ocrRequestActivePoolSize) {
        this.ocrRequestActivePoolSize = ocrRequestActivePoolSize;
    }

    public int getOcrRequestLargestPoolSize() {
        return ocrRequestLargestPoolSize;
    }

    public void setOcrRequestLargestPoolSize(int ocrRequestLargestPoolSize) {
        this.ocrRequestLargestPoolSize = ocrRequestLargestPoolSize;
    }

    public int getOcrRequestPoolSize() {
        return ocrRequestPoolSize;
    }

    public void setOcrRequestPoolSize(int ocrRequestPoolSize) {
        this.ocrRequestPoolSize = ocrRequestPoolSize;
    }

    public int getOcrRequestQueueSize() {
        return ocrRequestQueueSize;
    }

    public void setOcrRequestQueueSize(int ocrRequestQueueSize) {
        this.ocrRequestQueueSize = ocrRequestQueueSize;
    }

    public int getTesseractPoolSize() {
        return tesseractPoolSize;
    }

    public void setTesseractPoolSize(int tesseractThreadPoolSize) {
        this.tesseractPoolSize = tesseractThreadPoolSize;
    }

    public int getTesseractLargestPoolSize() {
        return tesseractLargestPoolSize;
    }

    public void setTesseractLargestPoolSize(int tesseractLargestPoolSize) {
        this.tesseractLargestPoolSize = tesseractLargestPoolSize;
    }

    public Map<String, Object> toMap() {

        // tesseractQueueSize, tesseractPoolSize, tesseractActivePoolSize (same for ocrRequest but queue should always be zero)

        Map<String, Object> map = new LinkedHashMap<>();

        map.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.STATISTICS_LOG_RECORD);
        map.put(JsonConstants.INSTANCE_UUID_NAME, instanceUuid);
        map.put(JsonConstants.TESSERACT_QUEUE_SIZE_NAME, tesseractQueueSize);
        map.put(JsonConstants.TESSERACT_ACTIVE_POOL_SIZE_NAME, tesseractActivePoolSize);
        map.put(JsonConstants.TESSERACT_POOL_SIZE_NAME, tesseractPoolSize);
        map.put(JsonConstants.TESSERACT_LARGEST_POOL_SIZE_NAME, tesseractLargestPoolSize);
        map.put(JsonConstants.OCR_REQUEST_QUEUE_SIZE_NAME, ocrRequestQueueSize);
        map.put(JsonConstants.OCR_REQUEST_ACTIVE_POOL_SIZE_NAME, ocrRequestActivePoolSize);
        map.put(JsonConstants.OCR_REQUEST_POOL_SIZE_NAME, ocrRequestPoolSize);
        map.put(JsonConstants.OCR_REQUEST_LARGEST_POOL_SIZE_NAME, ocrRequestLargestPoolSize);

        return  map;        
    }

}
