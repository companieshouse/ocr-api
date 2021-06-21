package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final int fileSize;


    public TextConversionResult(String contextId, String responseId, long timeOnExecuterQueue, int fileSize) {
        this.contextId = contextId;
        this.responseId = responseId;
        this.timeOnExecuterQueue = timeOnExecuterQueue;
        this.fileSize = fileSize;
        extractTextWatch.start();
    }

    public static TextConversionResult createForZeroLengthFile(String contextId, String responseId, long timeOnExecuterQueue) {
        return new TextConversionResult(contextId, responseId, timeOnExecuterQueue, 0);
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

        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("timeOnExecuterQueue", Long.valueOf(timeOnExecuterQueue));
        metadata.put("extractTextWatch", extractTextWatch.toString());
        metadata.put("totalPages", pageConfidences.size());
        metadata.put("pageConfidences", pageConfidences);
        metadata.put("documentConfidence", documentConfidence);
        metadata.put("fileSize", fileSize);

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

    public int getFileSize() {
        return fileSize;
    }
    
}
