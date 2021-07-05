package uk.gov.companieshouse.ocr.api.image.extracttext;

import static uk.gov.companieshouse.ocr.api.TestObjectMother.CONTEXT_ID;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.RESPONSE_ID;
import static uk.gov.companieshouse.ocr.api.TestObjectMother.TIME_PROCESSING;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.jupiter.api.Test;

class ExtractTextResultDtoTest {

    @Test
    void testCreateErrorExtractTextResultDTOFromContextId() {
        ExtractTextResultDto extractTextResultDto = ExtractTextResultDto.createErrorExtractTextResultDtoFromContextId(CONTEXT_ID, RESPONSE_ID, OcrRequestException.ResultCode.UNEXPECTED_FAILURE, TIME_PROCESSING);

        assertThat(extractTextResultDto.getAverageConfidenceScore(), is(0));
        assertThat(extractTextResultDto.getLowestConfidenceScore(), is(0));
        assertThat(extractTextResultDto.getOcrProcessingTimeMs(), is(0L));
        assertThat(extractTextResultDto.getTotalProcessingTimeMs(), is(TIME_PROCESSING));
        assertThat(extractTextResultDto.getContextId(), is(CONTEXT_ID));
        assertThat(extractTextResultDto.getResponseId(), is(RESPONSE_ID));
        assertThat(extractTextResultDto.getExtractedText(), is(ExtractTextResultDto.OCR_CONVERSION_ERROR_TEXT));
        assertThat(extractTextResultDto.getResultCode(), is(OcrRequestException.ResultCode.UNEXPECTED_FAILURE.getCode()));
    }
}
