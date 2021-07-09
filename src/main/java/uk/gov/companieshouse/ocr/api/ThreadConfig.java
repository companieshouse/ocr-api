package uk.gov.companieshouse.ocr.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Configuration
@EnableAsync
public class ThreadConfig {

    public static final String IMAGE_TO_TEXT_TASK_EXECUTOR_BEAN = "imageToTextTaskExecutor";
    public static final String OCR_REQUEST_EXECUTOR_BEAN = "ocrRequestTaskExecutor"; 

    private static final String IMAGE_TO_TEXT_THREAD_NAME_PREFIX= "image-to-text-thread-";
    private static final String OCR_REQUEST_THREAD_NAME_PREFIX= "ocr-request-thread-";

    private static final int DEFAULT_TESSERACT_THREAD_POOL_SIZE = 4; 

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    private EnvironmentReader reader = new EnvironmentReaderImpl();

    private final int threadPoolSize;

    public ThreadConfig() {
        this.threadPoolSize = findThreadPoolSize();
    }

    private int findThreadPoolSize() {

        var configuredThreadPoolSize = reader.getOptionalInteger("OCR_TESSERACT_THREAD_POOL_SIZE");
        return (configuredThreadPoolSize != null) ? configuredThreadPoolSize :  DEFAULT_TESSERACT_THREAD_POOL_SIZE;
    }

    @Bean (name=IMAGE_TO_TEXT_TASK_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor imageToTextTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        var processors = Runtime.getRuntime().availableProcessors();
        LOG.info("Using a thread pool of [" + threadPoolSize + "] with available processors of [" + processors + "] for " + IMAGE_TO_TEXT_TASK_EXECUTOR_BEAN);

        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setThreadNamePrefix(IMAGE_TO_TEXT_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }

    @Bean (name=OCR_REQUEST_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor ocrRequestTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        var processors = Runtime.getRuntime().availableProcessors();
        LOG.info("Using a thread pool of [" + threadPoolSize + "] with available processors of [" + processors + "] for " + OCR_REQUEST_EXECUTOR_BEAN);

        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setThreadNamePrefix(OCR_REQUEST_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

}