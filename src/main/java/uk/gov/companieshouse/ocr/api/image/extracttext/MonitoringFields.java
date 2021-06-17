package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.LinkedHashMap;
import java.util.Map;

public class MonitoringFields {


    private final int averageConfidenceScore;

    private final int lowestConfidenceScore;

    private final long ocrProcessingTimeMs;

    private final long totalProcessingTimeMs;   

    private final long timeOnExecuterQueue;

    private final Integer totalPages;

    private final int fileSize;

    public MonitoringFields(TextConversionResult textConversionResult, ExtractTextResultDto extractTextResultDto) {

        this.averageConfidenceScore = extractTextResultDto.getAverageConfidenceScore();
        this.lowestConfidenceScore = extractTextResultDto.getLowestConfidenceScore();
        this.ocrProcessingTimeMs = extractTextResultDto.getOcrProcessingTimeMs();
        this.totalProcessingTimeMs = extractTextResultDto.getTotalProcessingTimeMs();
        this.timeOnExecuterQueue = textConversionResult.getTimeOnExecuterQueue();
        this.totalPages = textConversionResult.getTotalPages();
        this.fileSize = textConversionResult.getFileSize();
    }

    public int getAverageConfidenceScore() {
        return averageConfidenceScore;
    }

    public int getLowestConfidenceScore() {
        return lowestConfidenceScore;
    }

    public long getOcrProcessingTimeMs() {
        return ocrProcessingTimeMs;
    }

    public long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs;
    }

    public long getTimeOnExecuterQueue() {
        return timeOnExecuterQueue;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public int getFileSize() {
        return fileSize;
    }

    public Map<String, Object> toMap() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("averageConfidenceScore", averageConfidenceScore);
        map.put("lowestConfidenceScore", lowestConfidenceScore);
        map.put("ocrProcessingTimeMs", ocrProcessingTimeMs);
        map.put("totalProcessingTimeMs", totalProcessingTimeMs);
        map.put("timeOnExecuterQueue", timeOnExecuterQueue);
        map.put("totalPages", totalPages);
        map.put("fileSize", fileSize);

        return  map;        
    }
    
}
