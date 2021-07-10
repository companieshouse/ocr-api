package uk.gov.companieshouse.ocr.api.statistics;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import uk.gov.companieshouse.ocr.api.ThreadConfig;

@Service
public class StatisticsService {

    private static final String INSTANCE_UUID = UUID.randomUUID().toString();

    @Autowired
    @Qualifier(ThreadConfig.OCR_REQUEST_EXECUTOR_BEAN)
    private ThreadPoolTaskExecutor ocrRequestTaskExecutor;

    public StatisticsDto generateStatistics() {

        StatisticsDto statistics = new StatisticsDto();

        statistics.setQueueSize(ocrRequestTaskExecutor.getThreadPoolExecutor().getQueue().size());
        statistics.setPoolSize(ocrRequestTaskExecutor.getThreadPoolExecutor().getPoolSize());
        statistics.setActivePoolSize(ocrRequestTaskExecutor.getThreadPoolExecutor().getActiveCount());
        statistics.setLargestPoolSize(ocrRequestTaskExecutor.getThreadPoolExecutor().getLargestPoolSize());

        statistics.setInstanceUuid(INSTANCE_UUID);

        return statistics;           
    }
    
}
