package uk.gov.companieshouse.ocr.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.ocr.api.common.JsonConstants;

@Configuration
@EnableAsync
public class ThreadConfig {

    public static final String OCR_REQUEST_EXECUTOR_BEAN = "ocrRequestTaskExecutor"; 

    private static final String OCR_REQUEST_THREAD_NAME_PREFIX= "ocr-request-thread-";

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

    @Bean (name=OCR_REQUEST_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor ocrRequestTaskExecutor(SpringConfiguration springConfiguration) {

        var executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setQueueCapacity(springConfiguration.getOcrQueueCapacity());
        executor.setThreadNamePrefix(OCR_REQUEST_THREAD_NAME_PREFIX);
        executor.initialize();

        var logMap = logMapThreadPoolConfig(executor);
        LOG.info("Creating ThreadPoolTaskExecutor for " + OCR_REQUEST_EXECUTOR_BEAN, logMap);

        return executor;
    }

    private Map<String, Object> logMapThreadPoolConfig(ThreadPoolTaskExecutor executor) {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put(JsonConstants.LOG_RECORD_NAME, JsonConstants.THREAD_POOL_EXECUTOR_CONFIG_LOG_RECORD);
        map.put(JsonConstants.QUEUE_CAPACITY_NAME, executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        map.put(JsonConstants.MAX_POOL_SIZE_NAME, executor.getThreadPoolExecutor().getMaximumPoolSize());
        map.put(JsonConstants.THREAD_NAME_PREFIX_NAME, executor.getThreadNamePrefix());
        map.put(JsonConstants.PROCESSORS_NAME, Runtime.getRuntime().availableProcessors());

        return  map;        
    }


    public int getThreadPoolSize() {
        return threadPoolSize;
    }

}