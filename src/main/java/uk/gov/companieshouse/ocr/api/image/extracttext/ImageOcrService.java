package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sun.jna.Pointer;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.ITessAPI.TessBaseAPI;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.util.ImageIOHelper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;

@Service
public class ImageOcrService {


    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Async
    public CompletableFuture<TextConversionResult> extractTextFromImage(MultipartFile file, String externalReferenceId, StopWatch timeOnQueueStopWatch) throws IOException, TesseractException {

        timeOnQueueStopWatch.stop();
        LOG.infoContext(externalReferenceId,"Time waiting on queue " + timeOnQueueStopWatch.toString(), null);

        final var textConversionResult = new TextConversionResult(timeOnQueueStopWatch.getTime()); 

        try(ImageInputStream is = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            ImageReader reader = createImageReader(is);
            extractTextFromImageViaApi(externalReferenceId, reader, textConversionResult);
        }

        return CompletableFuture.completedFuture(textConversionResult);
    }

    // pass in result (final)
    private void extractTextFromImageViaApi(String externalReferenceId, ImageReader reader, TextConversionResult textConversionResult) {
        TessAPI api = null;
        TessBaseAPI handle = null;

        try {
            api = TessAPI.INSTANCE;
            handle = TessAPI.INSTANCE.TessBaseAPICreate();

            api.TessBaseAPIInit3(handle, "/usr/share/tessdata/", "eng");

            int totalPages = reader.getNumImages(true); // add to results
            textConversionResult.setTotalPages(totalPages);

            LOG.debug("Number of pages to process " + totalPages);

            StringBuilder documentText = new StringBuilder();
            for (int currentPage = 0; currentPage < totalPages; currentPage++) {

                textConversionResult.addPage();
                LOG.info("Processed " + (currentPage * 100) / totalPages + "%");
                BufferedImage image = reader.read(currentPage);
                ByteBuffer buf = ImageIOHelper.convertImageData(image);
                int bpp = image.getColorModel().getPixelSize();
                int bytespp = bpp / 8;
                int bytespl = (int) Math.ceil(image.getWidth() * bpp / 8.0);
                api.TessBaseAPISetImage(handle, buf, image.getWidth(), image.getHeight(), bytespp, bytespl);
                Pointer utf8Text = api.TessBaseAPIGetUTF8Text(handle);
                String pageText = utf8Text.getString(0);
                api.TessDeleteText(utf8Text);
                ITessAPI.TessResultIterator ri = api.TessBaseAPIGetIterator(handle);
                int level = TessPageIteratorLevel.RIL_TEXTLINE;
  
                if (ri != null) {
                    do {
                        Pointer symbol = api.TessResultIteratorGetUTF8Text(ri, level);
                        float confidence = api.TessResultIteratorConfidence(ri, level);
                        if (symbol != null) {
                            textConversionResult.addConfidence(confidence);
                            LOG.trace("Confidence is " + confidence + " for " + symbol.getString(0));
                        } else {
                            LOG.debug("Confidence is " + confidence + " for blank line" );
                        }
                    } while (api.TessResultIteratorNext(ri, level) == ITessAPI.TRUE);
                }
                documentText.append(pageText);
            }

            textConversionResult.completeSuccess(documentText.toString());

            LOG.infoContext(externalReferenceId,"Document MetaData", textConversionResult.metaDataMap());

        } catch (IOException ex) {
            var tce = new TextConversionException(externalReferenceId, ex);
            throw tce;
        } finally {
            api.TessBaseAPIDelete(handle);
        }
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
        ImageReader reader = (ImageReader) iterator.next();
        iterator = null;
        reader.setInput(is);
        return reader;
    }
        
}