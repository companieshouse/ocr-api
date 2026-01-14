package uk.gov.companieshouse.ocr.api.image.extracttext;

import com.sun.jna.Pointer;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITessAPI.TessBaseAPI;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.util.ImageIOHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.SpringConfiguration;
import uk.gov.companieshouse.ocr.api.common.JsonConstants;
import uk.gov.companieshouse.ocr.api.tesseract.TesseractConstants;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ImageOcrService {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);
    private final int lowConfidenceToLog;

    @Autowired
    public ImageOcrService(SpringConfiguration springConfiguration) {
        lowConfidenceToLog = springConfiguration.getLowConfidenceToLog();
    }

    /**
     * 
     * @param contextId - context that the request is running in
     * @param imageBytes - bytes of the Tiff to convert
     * @param responseId - used in the result
     * @param timeOnQueue - time on executor queue (ms()
     * @return
     * @throws IOException - when it fails to convert the Image to Text (hard to simulate)
     * @throws TextConversionException - When you try to convert a bad file (e.g. zero length)
     */
    public TextConversionResult
    extractTextFromImageBytes(String contextId, byte[] imageBytes, String responseId, long timeOnQueue) throws IOException, TextConversionException {

        if (imageBytes.length == 0) {
            return TextConversionResult.createForZeroLengthFile(contextId, responseId, timeOnQueue);
        }

        final var textConversionResult = new TextConversionResult(contextId, responseId, timeOnQueue, imageBytes.length); 

        try(ImageInputStream is = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            ImageReader reader = createImageReader(is);
            extractTextFromImageViaApi(reader, textConversionResult);
        } 

        return textConversionResult;
    }


    private void extractTextFromImageViaApi(ImageReader reader, TextConversionResult textConversionResult) throws TextConversionException {
        TessAPI api = null;
        TessBaseAPI handle = null;

        try {
            api = TessAPI.INSTANCE;
            handle = TessAPI.INSTANCE.TessBaseAPICreate();

            api.TessBaseAPIInit3(handle, springConfiguration.getTessdataPath(), TesseractConstants.ENGLISH_LANGUAGE);

            int totalPages = reader.getNumImages(true); 

            LOG.debugContext(textConversionResult.getContextId(), "Number of pages to process " + totalPages, null);

            StringBuilder documentText = new StringBuilder();
            for (int currentPage = 0; currentPage < totalPages; currentPage++) {

                textConversionResult.addPage();

                LOG.infoContext(textConversionResult.getContextId(), "Processed " + (currentPage * 100) / totalPages + "%", null);

                documentText.append(extractTextOnCurrentPage(reader, textConversionResult, api, handle, currentPage));
            }

            textConversionResult.completeSuccess(documentText.toString());

            LOG.infoContext(textConversionResult.getContextId(),"Document MetaData", textConversionResult.metaDataMap());

        } catch (IOException ex) {
            throw new TextConversionException(textConversionResult.getContextId(), textConversionResult.getResponseId(), ex);
        } finally {
            api.TessBaseAPIDelete(handle);
        }
    }

    private String extractTextOnCurrentPage(ImageReader reader, TextConversionResult textConversionResult, TessAPI api,
            TessBaseAPI handle, int currentPage) throws IOException {

        var image = reader.read(currentPage);
        var buf = ImageIOHelper.convertImageData(image);
        var bpp = image.getColorModel().getPixelSize();
        var bytespp = bpp / 8;
        var bytespl = (int) Math.ceil(image.getWidth() * bpp / 8.0);

        api.TessBaseAPISetImage(handle, buf, image.getWidth(), image.getHeight(), bytespp, bytespl);
        var utf8TextPtr = api.TessBaseAPIGetUTF8Text(handle);
        var pageText = utf8TextPtr.getString(0);
        api.TessDeleteText(utf8TextPtr);

        var ri = api.TessBaseAPIGetIterator(handle);
        var level = TessPageIteratorLevel.RIL_TEXTLINE;
  
        if (ri != null) {
            var lineNum = 0;
            do {
                lineNum++;
                var symbol = api.TessResultIteratorGetUTF8Text(ri, level);
                var confidence = api.TessResultIteratorConfidence(ri, level);
                if (symbol != null) {
                    var lineText = getStringFromPointer(symbol);
                    textConversionResult.addConfidence(confidence);
                    if (confidence <= lowConfidenceToLog) {
                        // using currentPage + 1 as currentPage starts at '0' in code but outside of code, '1' is the accepted starting page number
                        var logDataMap = createLowConfidenceScoreLogMap(currentPage + 1, lineNum, confidence, lineText);
                        LOG.debugContext(textConversionResult.getContextId(), "Low confidence score", logDataMap);
                    }
                    LOG.traceContext(textConversionResult.getContextId(), "Confidence is " + confidence + " for " + lineText, null);
                } else {
                    LOG.debugContext(textConversionResult.getContextId(), "Confidence is " + confidence + " for blank line", null);
                }
            } while (api.TessResultIteratorNext(ri, level) == ITessAPI.TRUE);
        }
        return pageText;
    }

    private String getStringFromPointer(Pointer pointer) {
        String string;

        try {
            string = pointer.getString(0);
        } catch (Exception e) {
            string = "";
        }

        return string;
    }

    private ImageReader createImageReader(ImageInputStream is) throws IOException {
        if (is == null || is.length() == 0) {
            throw new IOException("Empty image input stream");
        }

        Iterator<ImageReader> iterator = ImageIO.getImageReaders(is);
        if (iterator == null || !iterator.hasNext()) {
            throw new IOException("No file readers found for image content (when using ImageIO)");
        }

        // Get first compatible reader
        var reader = iterator.next();
        reader.setInput(is);
        return reader;
    }

    private Map<String, Object> createLowConfidenceScoreLogMap(int pageNumber, int lineNumber, float confidence, String convertedText) {
        Map<String, Object> logDataMap = new LinkedHashMap<>();
        logDataMap.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.LOW_CONFIDENCE_LOG_RECORD);
        logDataMap.put(JsonConstants.PAGE_NUMBER_NAME, pageNumber);
        logDataMap.put(JsonConstants.LINE_NUMBER_NAME, lineNumber);
        logDataMap.put(JsonConstants.CONFIDENCE_VALUE_NAME, confidence);
        logDataMap.put(JsonConstants.CONVERTED_TEXT_NAME, convertedText);

        return logDataMap;
    }
}
