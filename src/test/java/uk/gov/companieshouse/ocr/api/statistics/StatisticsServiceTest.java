package uk.gov.companieshouse.ocr.api.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    ThreadPoolExecutor mockTesseractThreadPoolExecutor;

    @Mock
    ThreadPoolExecutor mockOcrRequestThreadPoolExecutor;

    @Mock
    BlockingQueue<Runnable>  mockTesseractBlockingQueue;

    @Mock
    BlockingQueue<Runnable>  mockOcrRequestBlockingQueue;

    @Mock
    private ThreadPoolTaskExecutor mockImageToTextTaskExecutor;

    @Mock
    private ThreadPoolTaskExecutor ocrRequestTaskExecutor;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void generateStatistics() {

        // given
        when(mockImageToTextTaskExecutor.getThreadPoolExecutor()).thenReturn(mockTesseractThreadPoolExecutor);
        when(mockImageToTextTaskExecutor.getThreadPoolExecutor().getQueue()).thenReturn(mockTesseractBlockingQueue);

        when(ocrRequestTaskExecutor.getThreadPoolExecutor()).thenReturn(mockOcrRequestThreadPoolExecutor);
        when(ocrRequestTaskExecutor.getThreadPoolExecutor().getQueue()).thenReturn(mockOcrRequestBlockingQueue);

        when(mockTesseractBlockingQueue.size()).thenReturn(10);
        when(mockTesseractThreadPoolExecutor.getPoolSize()).thenReturn(2);
        when(mockTesseractThreadPoolExecutor.getActiveCount()).thenReturn(1);
        when(mockTesseractThreadPoolExecutor.getLargestPoolSize()).thenReturn(4);

        when(mockOcrRequestBlockingQueue.size()).thenReturn(20);
        when(mockOcrRequestThreadPoolExecutor.getPoolSize()).thenReturn(12);
        when(mockOcrRequestThreadPoolExecutor.getActiveCount()).thenReturn(11);
        when(mockOcrRequestThreadPoolExecutor.getLargestPoolSize()).thenReturn(14);

        // when
        StatisticsDto statistics = statisticsService.generateStatistics();

        // then
        assertNotNull(statistics.getInstanceUuid());

        assertEquals(10, statistics.getTesseractQueueSize());
        assertEquals(2, statistics.getTesseractPoolSize());
        assertEquals(1, statistics.getTesseractActivePoolSize());
        assertEquals(4, statistics.getTesseractLargestPoolSize());

        assertEquals(20, statistics.getOcrRequestQueueSize());
        assertEquals(12, statistics.getOcrRequestPoolSize());
        assertEquals(11, statistics.getOcrRequestActivePoolSize());
        assertEquals(14, statistics.getOcrRequestLargestPoolSize());
    }

}
