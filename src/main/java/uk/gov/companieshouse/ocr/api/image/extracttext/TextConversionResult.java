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

    private List<Confidence> pageConfidences = new ArrayList<Confidence>();

    private Integer totalPages;

    private StopWatch extractTextWatch = new StopWatch();

    private final long timeOnExecuterQueue; 
    private final String responseId;


    public TextConversionResult(String responseId, long timeOnExecuterQueue) {
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

    public Map<String, Object> metaDataMap() {

        var metadata = new HashMap<String, Object>();

        metadata.put("timeOnExecuterQueue", Long.valueOf(timeOnExecuterQueue));
        metadata.put("extractTextWatch", extractTextWatch.toString());
        metadata.put("totalPages", totalPages);
        metadata.put("documentConfidence", documentConfidence);
        metadata.put("pageConfidences", pageConfidences);

        return  metadata;        
    }
    
    public long getTimeOnExecuterQueue() {
        return timeOnExecuterQueue;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }


	public void addConfidence(float confidence) {
        documentConfidence.addConfidenceValue(confidence);
        currentPageConfidence.addConfidenceValue(confidence);
	}

	public void addPage() {
        currentPageConfidence = new Confidence();
        pageConfidences.add(currentPageConfidence);
    }

    public Confidence getDocumentConfidence() {
        return documentConfidence;
    }

    public long getExtractTextProcessingTime() {
        return extractTextWatch.getTime();
    }

    public String getResponseId() {
        return responseId;
    }
    
}
