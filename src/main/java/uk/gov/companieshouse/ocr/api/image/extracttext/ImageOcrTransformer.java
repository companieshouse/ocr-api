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

         extractTextResultDTO.setAverageConfidenceScore(Math.round(textConversionResult.getDocumentConfidence().getAverage()));

         extractTextResultDTO.setExtractedText(textConversionResult.getExtractedText());

         extractTextResultDTO.setLowestConfidenceScore(Math.round(textConversionResult.getDocumentConfidence().getMinimum()));

         extractTextResultDTO.setProcessingTimeMs(textConversionResult.getExtractTextProcessingTime());


        return extractTextResultDTO;
    }
    
}
