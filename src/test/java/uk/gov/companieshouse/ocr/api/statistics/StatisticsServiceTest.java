package uk.gov.companieshouse.ocr.api.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.companieshouse.ocr.api.ThreadConfig;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

    private final static int TEST_TESSERACT_POOL_SIZE = 3;

    @Mock
    private ThreadConfig mockThreadConfig;

    @Mock
    private ThreadPoolTaskExecutor mockTaskExecutor;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void create() {

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        when(mockTaskExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(mockThreadConfig.getThreadPoolSize()).thenReturn(TEST_TESSERACT_POOL_SIZE);

        StatisticsDTO statistics = statisticsService.create();

        assertNotNull(statistics.getInstanceUuid());
        assertEquals(0, statistics.getQueueSize());
        assertEquals(TEST_TESSERACT_POOL_SIZE, statistics.getTesseractThreadPoolSize());
    }

}
