package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;

/**
 * Domain Class to contain the result of a Text conversion  
 */
public class TextConversionResult {

    private String extractedText;
    
    private Confidence documentConfidence = new Confidence();
    private Confidence currentPageConfidence;

    /**
     *  pageConfidences - one per page
     */
    private List<Confidence> pageConfidences = new ArrayList<>();

    private StopWatch extractTextWatch = new StopWatch();

    private final String contextId;
    private final long timeOnExecuterQueue; 
    private final String responseId;


    public TextConversionResult(String contextId, String responseId, long timeOnExecuterQueue) {
        this.contextId = contextId;
        this.responseId = responseId;
        this.timeOnExecuterQueue = timeOnExecuterQueue;
        extractTextWatch.start();
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void completeSuccess(String extractedText) {
        this.extractedText = extractedText;
        extractTextWatch.stop();
    }

    // Useful for logging the metadata via Structured logging
    public Map<String, Object> metaDataMap() {

        var metadata = new HashMap<String, Object>();

        metadata.put("timeOnExecuterQueue", Long.valueOf(timeOnExecuterQueue));
        metadata.put("extractTextWatch", extractTextWatch.toString());
        metadata.put("totalPages", pageConfidences.size());
        metadata.put("documentConfidence", documentConfidence);
        metadata.put("pageConfidences", pageConfidences);

        return  metadata;        
    }
    
    public long getTimeOnExecuterQueue() {
        return timeOnExecuterQueue;
    }

    public Integer getTotalPages() {
        return pageConfidences.size();
    }

	public void addConfidence(float confidence) {
        documentConfidence.addConfidenceValue(confidence);
        currentPageConfidence.addConfidenceValue(confidence);
	}

	public void addPage() {
        currentPageConfidence = new Confidence();
        pageConfidences.add(currentPageConfidence);
    }

    public Float getDocumentAverageConfidence() {
        return documentConfidence.getAverage();
    }

    public Float getDocumentMinimumConfidence() {
        return documentConfidence.getMinimum();
    }

    public long getExtractTextProcessingTime() {
        return extractTextWatch.getTime();
    }

    public String getResponseId() {
        return responseId;
    }

    public String getContextId() {
        return contextId;
    }
    
}
