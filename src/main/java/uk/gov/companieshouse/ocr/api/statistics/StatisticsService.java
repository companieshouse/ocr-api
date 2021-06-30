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
    private ThreadConfig threadConfig;

    @Autowired
    @Qualifier(ThreadConfig.IMAGE_TO_TEXT_TASK_EXECUTOR_BEAN)
    private ThreadPoolTaskExecutor imageToTextTaskExecutor;

    public StatisticsDto generateStatistics() {

        StatisticsDto statistics = new StatisticsDto();
        statistics.setQueueSize(imageToTextTaskExecutor.getThreadPoolExecutor().getQueue().size());
        statistics.setTesseractThreadPoolSize(threadConfig.getThreadPoolSize());
        statistics.setInstanceUuid(INSTANCE_UUID);

        return statistics;           
    }
    
}
