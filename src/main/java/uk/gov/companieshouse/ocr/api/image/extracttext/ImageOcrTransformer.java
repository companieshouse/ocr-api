package uk.gov.companieshouse.ocr.api.image.extracttext;

public class ImageOcrTransformer {

    public ExtractTextResultDTO mapModelToApi(TextConversionResult textConversionResult) {
 
        var extractTextResultDTO = new ExtractTextResultDTO();

        extractTextResultDTO.setExtractedText(textConversionResult.getExtractedText());

        extractTextResultDTO.setAverageConfidenceScore(Math.round(textConversionResult.getDocumentAverageConfidence()));
        extractTextResultDTO.setLowestConfidenceScore(Math.round(textConversionResult.getDocumentMinimumConfidence()));

        extractTextResultDTO.setOcrProcessingTimeMs(textConversionResult.getExtractTextProcessingTime());

        extractTextResultDTO.setResponseId(textConversionResult.getResponseId());

        return extractTextResultDTO;
    }
    
}
