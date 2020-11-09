package uk.gov.companieshouse.ocr.api.image.extracttext;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.TesseractException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.OcrApiApplication;

@RestController
public class ImageOcrApiController {

    public static final String TIFF_EXTRACT_TEXT_PARTIAL_URL = "/api/ocr/image/tiff/extractText";

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Autowired
    private ImageOcrService imageOcrService;

    @PostMapping(TIFF_EXTRACT_TEXT_PARTIAL_URL)
    public @ResponseBody ResponseEntity<ExtractTextResultDTO> extractTextFromImageFileViaAPI(
            @RequestParam("file") MultipartFile file, @RequestParam("responseId") String responseId) throws IOException, TesseractException {

        var controllerStopWatch = new StopWatch();
        controllerStopWatch.start();
        
        LOG.infoContext(responseId,"Processing file [" + file.getOriginalFilename() + "] Content type [" + file.getContentType() + "]", null);

        var timeOnQueueStopWatch = new StopWatch();
        timeOnQueueStopWatch.start();
        var result = ImageOcrTransformer.mapModelToApi( imageOcrService.extractTextFromImage(file, responseId, timeOnQueueStopWatch).join());

        controllerStopWatch.stop();
        result.setTotalProcessingTimeMs(controllerStopWatch.getTime());
    
        LOG.infoContext(responseId, "Finished processing file " + file.getOriginalFilename() + " - time to run " + (controllerStopWatch.getTime()) + " (ms) " + "[ " +
           controllerStopWatch.toString() + "]", null);

        return new ResponseEntity<ExtractTextResultDTO>(result, HttpStatus.OK);
    }

    /*
     Occurs when the  `.join()` method is called after calling an `async` method AND an untrapped exception is thown within that method
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<Object> handleCompletionException(CompletionException e) {

        if (e.getCause() instanceof TextConversionException) {

            var cause = (TextConversionException) e.getCause();
            logError(cause.getResponseId(), cause);
        }
        else {
            logError(null, e);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> uncaughtException(Exception e) {

        LOG.error(null, e);

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logError(String responseId, Exception e) {

        LOG.errorContext(responseId,  e, null);
    }

}
