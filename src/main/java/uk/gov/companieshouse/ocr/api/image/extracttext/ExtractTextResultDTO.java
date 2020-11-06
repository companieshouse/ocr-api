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

    @JsonProperty("extracted-text")
    private String extractedText;

    private int averageConfidenceScore;

    private int lowestConfidenceScore;

    private long processingTimeMs;

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

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    
}