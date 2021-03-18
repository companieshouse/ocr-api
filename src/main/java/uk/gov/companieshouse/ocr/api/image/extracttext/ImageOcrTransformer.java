package uk.gov.companieshouse.ocr.api.image.extracttext;

public class ImageOcrTransformer {

    public ExtractTextResultDto mapModelToApi(TextConversionResult textConversionResult) {
 
        var extractTextResultDto = new ExtractTextResultDto();

        extractTextResultDto.setExtractedText(textConversionResult.getExtractedText());

        extractTextResultDto.setAverageConfidenceScore(Math.round(textConversionResult.getDocumentAverageConfidence()));
        extractTextResultDto.setLowestConfidenceScore(Math.round(textConversionResult.getDocumentMinimumConfidence()));

        extractTextResultDto.setOcrProcessingTimeMs(textConversionResult.getExtractTextProcessingTime());

        extractTextResultDto.setContextId(textConversionResult.getContextId());
        extractTextResultDto.setResponseId(textConversionResult.getResponseId());

        return extractTextResultDto;
    }
    
}
