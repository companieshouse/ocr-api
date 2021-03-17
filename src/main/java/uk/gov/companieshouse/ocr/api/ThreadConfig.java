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

    private static final int DEFAULT_THREAD_POOL_SIZE = 4; 

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    private EnvironmentReader reader = new EnvironmentReaderImpl();

    private final int threadPoolSize;

    public ThreadConfig() {
        this.threadPoolSize = findThreadPoolSize();
    }

    private int findThreadPoolSize() {

        var configuredThreadPoolSize = reader.getOptionalInteger("OCR_TESSERACT_THREAD_POOL_SIZE");
        return (configuredThreadPoolSize != null) ? configuredThreadPoolSize :  DEFAULT_THREAD_POOL_SIZE;
    }

    @Bean (name="taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        var processors = Runtime.getRuntime().availableProcessors();
        LOG.info("Using a thread pool of [" + threadPoolSize + "] with available processors of [" + processors + "]");

        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setThreadNamePrefix("task_executor_thread");
        executor.initialize();
        return executor;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

}