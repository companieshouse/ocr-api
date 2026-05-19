package uk.gov.companieshouse.ocr.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
class RestTemplateConfigTest {
    private RestTemplateConfig testConfig;

    private RestTemplateBuilder builder;

    private SpringConfiguration springConfiguration;

    @BeforeEach
    void setupTestConfig() {
        testConfig = new RestTemplateConfig();
        builder = new RestTemplateBuilder();
        springConfiguration = mock(SpringConfiguration.class);
    }

    @Test
    void whenRestTemplateBeanRequested_thenValuesCorrectlySet() {
        when(springConfiguration.getRestClientTimeoutSeconds()).thenReturn(76);

        final var restTemplateBean = testConfig.restTemplate(builder, springConfiguration);

        assertNotNull(restTemplateBean);
    }
}