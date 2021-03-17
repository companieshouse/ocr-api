package uk.gov.companieshouse.ocr.api.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import uk.gov.companieshouse.ocr.api.ThreadConfig;

@Service
public class StatisticsService {

    @Autowired
    private ThreadConfig threadConfig;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    public StatisticsDTO create() {

        StatisticsDTO statistics = new StatisticsDTO();
        statistics.setQueueSize(taskExecutor.getThreadPoolExecutor().getQueue().size());
        statistics.setTesseractThreadPoolSize(threadConfig.getThreadPoolSize());

        return statistics;           
    }
    
}
