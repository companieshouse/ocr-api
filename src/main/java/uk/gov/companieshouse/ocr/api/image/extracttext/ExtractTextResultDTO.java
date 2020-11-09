package uk.gov.companieshouse.ocr.api.image.extracttext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Results returned to a client on a successful API call { "extracted-text":
 * "This is the extracted extracted from the document",
 * "average-confidence-score": "96", "lowest-confidence-score": "45",
 * "processing-time-ms": "27363" }
 * 
 */
public class ExtractTextResultDTO {

    @JsonProperty("extracted_text")
    private String extractedText;

    @JsonProperty("average_confidence_score")
    private int averageConfidenceScore;

    @JsonProperty("lowest_confidence_score")
    private int lowestConfidenceScore;

    @JsonProperty("ocr_processing_time_ms")
    private long ocrProcessingTimeMs;

    @JsonProperty("total_processing_time_ms")
    private long totalProcessingTimeMs;   

    @JsonProperty("response_id ")
    private String responseId;

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public int getAverageConfidenceScore() {
        return averageConfidenceScore;
    }

    public void setAverageConfidenceScore(int averageConfidenceScore) {
        this.averageConfidenceScore = averageConfidenceScore;
    }

    public int getLowestConfidenceScore() {
        return lowestConfidenceScore;
    }

    public void setLowestConfidenceScore(int lowestConfidenceScore) {
        this.lowestConfidenceScore = lowestConfidenceScore;
    }

    public long getOcrProcessingTimeMs() {
        return ocrProcessingTimeMs;
    }

    public void setOcrProcessingTimeMs(long ocrProcessingTimeMs) {
        this.ocrProcessingTimeMs = ocrProcessingTimeMs;
    }

    public long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs;
    }

    public void setTotalProcessingTimeMs(long totalProcessingTimeMs) {
        this.totalProcessingTimeMs = totalProcessingTimeMs;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }
    
}
