package uk.gov.companieshouse.ocr.api.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
class OpenTelemetryAppenderInitializerTest {

    @Test
    void afterPropertiesSetDelegatesToInstallAppender() {
        OpenTelemetry openTelemetry = OpenTelemetry.noop();
        OpenTelemetryAppenderInitializer initializer =
                spy(new OpenTelemetryAppenderInitializer(openTelemetry));

        // Stub out the seam so the real static
        // OpenTelemetryAppender.install(...) call, and its global
        // JVM logging side effect, is never invoked in this test.
        doNothing().when(initializer).installAppender(openTelemetry);

        initializer.afterPropertiesSet();

        verify(initializer).installAppender(openTelemetry);
    }

    @Test
    void installAppenderDelegatesToStaticOpenTelemetryAppenderInstall() {
        OpenTelemetry openTelemetry = OpenTelemetry.noop();
        OpenTelemetryAppenderInitializer initializer =
               new OpenTelemetryAppenderInitializer(openTelemetry);

        // Exercises the real static OpenTelemetryAppender.install(...)
        // call so the seam method itself is covered, rather than only
        // the delegation from afterPropertiesSet.
        assertDoesNotThrow(() -> initializer.installAppender(openTelemetry));
    }

}
