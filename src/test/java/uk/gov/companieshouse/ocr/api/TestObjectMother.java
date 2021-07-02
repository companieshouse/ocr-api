package uk.gov.companieshouse.ocr.api;

import java.time.LocalDateTime;
import java.time.Month;

import uk.gov.companieshouse.ocr.api.image.extracttext.OcrClientRequest;
import uk.gov.companieshouse.ocr.api.image.extracttext.OcrRequest;
import uk.gov.companieshouse.ocr.api.image.extracttext.TextConversionResult;

public final class TestObjectMother {

    public static final String APPLICATION_ID = "test";
    public static final String CONTEXT_ID = "XYZ";
    public static final String RESPONSE_ID = "ABC";
    public static final String IMAGE_ENDPOINT = "https://image-endpoint";
    public static final String EXTRACTED_TEXT_ENDPOINT = "https://converted-text-endpoint";
    public static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of (2021, Month.JULY, 1, 14, 29, 20, 1);

    public static final byte[] MOCK_TIFF_CONTENT = {0, 1, 2};

    private static final long TIME_ON_EXECUTOR_QUEUE = 210l;
    private static final String TEXT_TEXT = "Boring Text for text results";
    private static final int FILE_SIZE = 12345;

    public static OcrRequest getStandardOcrRequest() {
        OcrClientRequest clientRequest = new OcrClientRequest();
        clientRequest.setApplicationId(APPLICATION_ID);
        clientRequest.setConvertedTextEndpoint(EXTRACTED_TEXT_ENDPOINT);
        clientRequest.setImageEndpoint(IMAGE_ENDPOINT);
        clientRequest.setResponseId(RESPONSE_ID);

        return new OcrRequest(clientRequest, TEST_DATE_TIME);
    }

    public static TextConversionResult getStandardTextConversionResult() {

        var textConversionResult = new TextConversionResult(CONTEXT_ID, RESPONSE_ID, TIME_ON_EXECUTOR_QUEUE, FILE_SIZE);

        textConversionResult.addPage();

        textConversionResult.addConfidence(20f);
        textConversionResult.addConfidence(90f);
        textConversionResult.addConfidence(83f);
        textConversionResult.addConfidence(66f);
        textConversionResult.addConfidence(99f);

        textConversionResult.completeSuccess(TEXT_TEXT);

        return textConversionResult;
    }

    private TestObjectMother(){};
    
}
