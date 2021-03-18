package uk.gov.companieshouse.ocr.api.statistics;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import uk.gov.companieshouse.ocr.api.ThreadConfig;

@Service
public class StatisticsService {

    private static final String INSTANCE_UUID = UUID.randomUUID().toString();

    @Autowired
    private ThreadConfig threadConfig;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    public StatisticsDto generateStatistics() {

        StatisticsDto statistics = new StatisticsDto();
        statistics.setQueueSize(taskExecutor.getThreadPoolExecutor().getQueue().size());
        statistics.setTesseractThreadPoolSize(threadConfig.getThreadPoolSize());
        statistics.setInstanceUuid(INSTANCE_UUID);

        return statistics;           
    }
    
}
