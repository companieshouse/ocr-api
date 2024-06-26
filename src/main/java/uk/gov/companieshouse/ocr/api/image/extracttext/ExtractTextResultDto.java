package uk.gov.companieshouse.ocr.api.image.extracttext;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.companieshouse.ocr.api.image.extracttext.OcrRequestException.ResultCode;

/**
 * Results returned to a client on a successful API call
 */
public class ExtractTextResultDto {

    protected static final String OCR_CONVERSION_ERROR_TEXT = "UNABLE_TO_PROCESS_OCR_CONVERSION";

    /**
     *  The input contextId of the OCR request
     */
    @JsonProperty("context_id")
    private String contextId;

    /**
     *  The text for the OCR request
     */
    @JsonProperty("extracted_text")
    private String extractedText;

    /**
     *  average value (line by line) of the Tesseract confidence (0 to 100) of the OCR conversion process
     */
    @JsonProperty("average_confidence_score")
    private int averageConfidenceScore;

    /**
     *  the lowest value (line by line) of the Tesseract confidence (0 to 100) of the OCR conversion process
     */
    @JsonProperty("lowest_confidence_score")
    private int lowestConfidenceScore;

    /**
     *  Time for the OCT Text conversion process itself 
     */
    @JsonProperty("ocr_processing_time_ms")
    private long ocrProcessingTimeMs;

    @JsonProperty("total_processing_time_ms")
    private long totalProcessingTimeMs;   

    /**
     *  The input responseId of the OCR request
     */
    @JsonProperty("response_id")
    private String responseId;

    /**
	 * To store the result of the OCR Service. The values will be:
	 * = 0 : SUCCESS
	 * > 0 : ERROR within the service
	 */
	@JsonProperty("result_code")
	private int resultCode;

    public static ExtractTextResultDto createErrorExtractTextResultDtoFromContextId(String contextId,
            String responseId, ResultCode errorResultCode, long totalProcessingTimeMs) {

        ExtractTextResultDto extractedTextError = new ExtractTextResultDto();
        extractedTextError.setContextId(contextId);
        extractedTextError.setAverageConfidenceScore(0);
        extractedTextError.setLowestConfidenceScore(0);
        extractedTextError.setOcrProcessingTimeMs(0L);
        extractedTextError.setTotalProcessingTimeMs(totalProcessingTimeMs);
        extractedTextError.setResponseId(responseId);
        extractedTextError.setExtractedText(OCR_CONVERSION_ERROR_TEXT);
        extractedTextError.setResultCode(errorResultCode.getCode());

        return extractedTextError;
    }    

    /*  ------ Accessors ------- */
    
    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

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

    public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

}
