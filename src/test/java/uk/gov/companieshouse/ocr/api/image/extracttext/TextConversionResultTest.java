package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
public class TextConversionResultTest {

    private final static String RESPONSE_ID = "test-response-id";
    private final static long TIME_ON_EXECUTOR_QUEUE = 210l;
    private final static String TEXT_TEXT = "Boring Text for text results";


    private TextConversionResult textConversionResult;

    @BeforeEach
    public void setup() {
        this.textConversionResult = new TextConversionResult(RESPONSE_ID, TIME_ON_EXECUTOR_QUEUE);
    }

    @Test
    public void singlePageResults5Datapoints() {

        textConversionResult.setTotalPages(1);

        textConversionResult.addPage();

        textConversionResult.addConfidence(20f);
        textConversionResult.addConfidence(90f);
        textConversionResult.addConfidence(83f);
        textConversionResult.addConfidence(66f);
        textConversionResult.addConfidence(99f);

        textConversionResult.completeSuccess(TEXT_TEXT);

        System.out.println(textConversionResult.metaDataMap());

        assertEquals(TEXT_TEXT, textConversionResult.getExtractedText());
        assertEquals(TIME_ON_EXECUTOR_QUEUE, textConversionResult.getTimeOnExecuterQueue());
        assertEquals(1, textConversionResult.getTotalPages());
        assertEquals(71.6f, textConversionResult.getDocumentAverageConfidence());
        assertEquals(20.0f, textConversionResult.getDocumentMinimumConfidence());
        assertEquals(RESPONSE_ID, textConversionResult.getResponseId());

        var metaDataMap = textConversionResult.metaDataMap();
        assertEquals(TIME_ON_EXECUTOR_QUEUE, metaDataMap.get("timeOnExecuterQueue"));
        assertEquals(1, metaDataMap.get("totalPages"));
        // TO FINISH TOMORROW assertEquals(1, metaDataMap.get("documentConfidence"));


    }

}
