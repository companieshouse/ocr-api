package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITessAPI.TessBaseAPI;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageIOHelper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;
import uk.gov.companieshouse.ocr.api.tesseract.TesseractConstants;

@Service
public class ImageOcrService {

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Async
    public CompletableFuture<TextConversionResult> 
    //public TextConversionResult
    extractTextFromImage(MultipartFile file, String responseId, StopWatch timeOnQueueStopWatch) throws IOException, TesseractException {

        timeOnQueueStopWatch.stop();
        LOG.infoContext(responseId, "Time waiting on queue " + timeOnQueueStopWatch.toString(), null);

        final var textConversionResult = new TextConversionResult(responseId, timeOnQueueStopWatch.getTime()); 

        try(ImageInputStream is = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            ImageReader reader = createImageReader(is);
            extractTextFromImageViaApi(reader, textConversionResult);
        }

        return CompletableFuture.completedFuture(textConversionResult);
    }

    private void extractTextFromImageViaApi(ImageReader reader, TextConversionResult textConversionResult) {
        TessAPI api = null;
        TessBaseAPI handle = null;

        try {
            api = TessAPI.INSTANCE;
            handle = TessAPI.INSTANCE.TessBaseAPICreate();

            api.TessBaseAPIInit3(handle, TesseractConstants.TRAINING_DATA_PATH, TesseractConstants.ENGLISH_LANGUAGE);

            int totalPages = reader.getNumImages(true); 
            textConversionResult.setTotalPages(totalPages);

            LOG.debugContext(textConversionResult.getResponseId(), "Number of pages to process " + totalPages, null);

            StringBuilder documentText = new StringBuilder();
            for (int currentPage = 0; currentPage < totalPages; currentPage++) {

                textConversionResult.addPage();

                LOG.infoContext(textConversionResult.getResponseId(), "Processed " + (currentPage * 100) / totalPages + "%", null);

                documentText.append(extractTextOnCurrentPage(reader, textConversionResult, api, handle, currentPage));
            }

            textConversionResult.completeSuccess(documentText.toString());

            LOG.infoContext(textConversionResult.getResponseId(),"Document MetaData", textConversionResult.metaDataMap());

        } catch (IOException ex) {
            var tce = new TextConversionException(textConversionResult.getResponseId(), ex);
            throw tce;
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
            do {
                var symbol = api.TessResultIteratorGetUTF8Text(ri, level);
                var confidence = api.TessResultIteratorConfidence(ri, level);
                if (symbol != null) {
                    textConversionResult.addConfidence(confidence);
                    LOG.trace("Confidence is " + confidence + " for " + symbol.getString(0));
                } else {
                    LOG.debug("Confidence is " + confidence + " for blank line" );
                }
            } while (api.TessResultIteratorNext(ri, level) == ITessAPI.TRUE);
        }
        return pageText;
    }

    private ImageReader createImageReader(ImageInputStream is) throws IOException {
        if (is == null || is.length() == 0) {
            throw new IOException("Invalid input stream");
        }

        Iterator<ImageReader> iterator = ImageIO.getImageReaders(is);
        if (iterator == null || !iterator.hasNext()) {
            throw new IOException("Image file format not supported by ImageIO: ");
        }

        // Get first compatible reader
        var reader = (ImageReader) iterator.next();
        iterator = null;
        reader.setInput(is);
        return reader;
    }
        
}