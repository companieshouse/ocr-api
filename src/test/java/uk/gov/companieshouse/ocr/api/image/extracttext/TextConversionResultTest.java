package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
class TextConversionResultTest {

    private static final String CONTEXT_ID = "test-context-id";
    private static final String RESPONSE_ID = "test-response-id";
    private static final long TIME_ON_EXECUTOR_QUEUE = 210l;
    private static final String TEXT_TEXT = "Boring Text for text results";


    private TextConversionResult textConversionResult;

    @BeforeEach
    public void setup() {
        this.textConversionResult = new TextConversionResult(CONTEXT_ID, RESPONSE_ID, TIME_ON_EXECUTOR_QUEUE);
    }

    @Test
    void singlePageResults5Datapoints() {

        textConversionResult.addPage();

        textConversionResult.addConfidence(20f);
        textConversionResult.addConfidence(90f);
        textConversionResult.addConfidence(83f);
        textConversionResult.addConfidence(66f);
        textConversionResult.addConfidence(99f);

        textConversionResult.completeSuccess(TEXT_TEXT);

        assertEquals(TEXT_TEXT, textConversionResult.getExtractedText());
        assertEquals(TIME_ON_EXECUTOR_QUEUE, textConversionResult.getTimeOnExecuterQueue());
        assertEquals(1, textConversionResult.getTotalPages());
        assertEquals(71.6f, textConversionResult.getDocumentAverageConfidence());
        assertEquals(20.0f, textConversionResult.getDocumentMinimumConfidence());
        assertEquals(RESPONSE_ID, textConversionResult.getResponseId());
        assertEquals(CONTEXT_ID, textConversionResult.getContextId());

        var metaDataMap = textConversionResult.metaDataMap();
        assertEquals(TIME_ON_EXECUTOR_QUEUE, metaDataMap.get("timeOnExecuterQueue"));
        assertEquals(1, metaDataMap.get("totalPages"));
        var expectedDocumentConfidence = "Confidence [average=71.6, minimum=20.0, numberOfDataPoints=5, sum=358.0]";
        assertEquals(expectedDocumentConfidence, metaDataMap.get("documentConfidence").toString());
        var expectedPageConfidences = "[Confidence [average=71.6, minimum=20.0, numberOfDataPoints=5, sum=358.0]]";
        assertEquals(expectedPageConfidences, metaDataMap.get("pageConfidences").toString());
    }

    @Test
    void multiPageResultsDatapoints() {

        textConversionResult.addPage();

        textConversionResult.addConfidence(20f);
        textConversionResult.addConfidence(90f);
        textConversionResult.addConfidence(83f);

        textConversionResult.addPage();

        textConversionResult.addConfidence(66f);
        textConversionResult.addConfidence(99f);

        textConversionResult.completeSuccess(TEXT_TEXT);

        assertEquals(TEXT_TEXT, textConversionResult.getExtractedText());
        assertEquals(TIME_ON_EXECUTOR_QUEUE, textConversionResult.getTimeOnExecuterQueue());
        assertEquals(2, textConversionResult.getTotalPages());
        assertEquals(71.6f, textConversionResult.getDocumentAverageConfidence());
        assertEquals(20.0f, textConversionResult.getDocumentMinimumConfidence());
        assertEquals(RESPONSE_ID, textConversionResult.getResponseId());
        assertEquals(CONTEXT_ID, textConversionResult.getContextId());

        var metaDataMap = textConversionResult.metaDataMap();
        assertEquals(TIME_ON_EXECUTOR_QUEUE, metaDataMap.get("timeOnExecuterQueue"));
        assertEquals(2, metaDataMap.get("totalPages"));
        var expectedDocumentConfidence = "Confidence [average=71.6, minimum=20.0, numberOfDataPoints=5, sum=358.0]";
        assertEquals(expectedDocumentConfidence, metaDataMap.get("documentConfidence").toString());
        var expectedPageConfidences = "[Confidence [average=64.333336, minimum=20.0, numberOfDataPoints=3, sum=193.0], Confidence [average=82.5, minimum=66.0, numberOfDataPoints=2, sum=165.0]]";
        assertEquals(expectedPageConfidences, metaDataMap.get("pageConfidences").toString());

    }

    @Test
    void multiPageResultsWithBlankPageDatapoints() {

        textConversionResult.addPage();

        textConversionResult.addConfidence(20f);
        textConversionResult.addConfidence(90f);
        textConversionResult.addConfidence(83f);

        textConversionResult.addPage(); // blank page

        textConversionResult.addPage();

        textConversionResult.addConfidence(66f);
        textConversionResult.addConfidence(99f);

        textConversionResult.completeSuccess(TEXT_TEXT);

        assertEquals(TEXT_TEXT, textConversionResult.getExtractedText());
        assertEquals(TIME_ON_EXECUTOR_QUEUE, textConversionResult.getTimeOnExecuterQueue());
        assertEquals(3, textConversionResult.getTotalPages());
        assertEquals(71.6f, textConversionResult.getDocumentAverageConfidence());
        assertEquals(20.0f, textConversionResult.getDocumentMinimumConfidence());
        assertEquals(RESPONSE_ID, textConversionResult.getResponseId());
        assertEquals(CONTEXT_ID, textConversionResult.getContextId());

        var metaDataMap = textConversionResult.metaDataMap();
        assertEquals(TIME_ON_EXECUTOR_QUEUE, metaDataMap.get("timeOnExecuterQueue"));
        assertEquals(3, metaDataMap.get("totalPages"));
        var expectedDocumentConfidence = "Confidence [average=71.6, minimum=20.0, numberOfDataPoints=5, sum=358.0]";
        assertEquals(expectedDocumentConfidence, metaDataMap.get("documentConfidence").toString());
        var expectedPageConfidences = "[Confidence [average=64.333336, minimum=20.0, numberOfDataPoints=3, sum=193.0], Confidence [average=null, minimum=null, numberOfDataPoints=0, sum=0.0], Confidence [average=82.5, minimum=66.0, numberOfDataPoints=2, sum=165.0]]";
        assertEquals(expectedPageConfidences, metaDataMap.get("pageConfidences").toString());

    }

    @Test
    void blankDocument() {

        textConversionResult.addPage();

        assertNull(textConversionResult.getExtractedText());
        assertEquals(TIME_ON_EXECUTOR_QUEUE, textConversionResult.getTimeOnExecuterQueue());
        assertEquals(1, textConversionResult.getTotalPages());
        assertNull(textConversionResult.getDocumentAverageConfidence());
        assertNull(textConversionResult.getDocumentMinimumConfidence());
        assertEquals(RESPONSE_ID, textConversionResult.getResponseId());
        assertEquals(CONTEXT_ID, textConversionResult.getContextId());

        var metaDataMap = textConversionResult.metaDataMap();
        assertEquals(TIME_ON_EXECUTOR_QUEUE, metaDataMap.get("timeOnExecuterQueue"));
        assertEquals(1, metaDataMap.get("totalPages"));
        var expectedDocumentConfidence = "Confidence [average=null, minimum=null, numberOfDataPoints=0, sum=0.0]";
        assertEquals(expectedDocumentConfidence, metaDataMap.get("documentConfidence").toString());
        var expectedPageConfidences = "[Confidence [average=null, minimum=null, numberOfDataPoints=0, sum=0.0]]";
        assertEquals(expectedPageConfidences, metaDataMap.get("pageConfidences").toString());
    }

}
