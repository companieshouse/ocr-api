package uk.gov.companieshouse.ocr.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Configuration
@EnableAsync
public class ThreadConfig {

    private final static int DEFAULT_THREAD_POOL_SIZE = 4; 

    private static final Logger LOG = LoggerFactory.getLogger(OcrApiApplication.APPLICATION_NAME_SPACE);

    @Bean (name="taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        var threadPoolSize = getThreadPoolSize();
        LOG.info("Using a thread pool of " + threadPoolSize);

        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setThreadNamePrefix("task_executor_thread");
        executor.initialize();
        return executor;
    }

    private int getThreadPoolSize() {

        var configuredThreadPoolSize = System.getenv("OCR_TESSERACT_THREAD_POOL_SIZE");
        int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

        try{
            threadPoolSize = Integer.parseInt(configuredThreadPoolSize);
        }  
           catch (NumberFormatException nfe)
        {
            LOG.error("Can not get the OCR_TESSERACT_THREAD_POOL_SIZE, using default = NumberFormatException: " + nfe.getMessage());
        }

        return threadPoolSize;
    }
}