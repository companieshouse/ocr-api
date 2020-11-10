package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.springframework.web.multipart.MultipartFile;

public class ImageOcrTransformer {

    public static ImageInputStream getStream(MultipartFile file) throws IOException {

        return  ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()));
    }

    public static ExtractTextResultDTO mapModelToApi(TextConversionResult textConversionResult) {
 
        var extractTextResultDTO = new ExtractTextResultDTO();

        extractTextResultDTO.setExtractedText(textConversionResult.getExtractedText());

        extractTextResultDTO.setAverageConfidenceScore(Math.round(textConversionResult.getDocumentAverageConfidence()));
        extractTextResultDTO.setLowestConfidenceScore(Math.round(textConversionResult.getDocumentMinimumConfidence()));

        extractTextResultDTO.setOcrProcessingTimeMs(textConversionResult.getExtractTextProcessingTime());

        extractTextResultDTO.setResponseId(textConversionResult.getResponseId());

        return extractTextResultDTO;
    }
    
}
