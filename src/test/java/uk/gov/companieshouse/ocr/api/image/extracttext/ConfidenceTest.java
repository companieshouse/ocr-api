package uk.gov.companieshouse.ocr.api.image.extracttext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import uk.gov.companieshouse.ocr.api.groups.TestType;

@Tag(TestType.UNIT)
class ConfidenceTest {

	private Confidence confidence;

    @BeforeEach
	public void setup() {
		this.confidence = new Confidence();
	}

	@Test
	void shouldaddThreeValuesOK() {
		
		confidence.addConfidenceValue(62.2f);
		confidence.addConfidenceValue(70.8f);
		confidence.addConfidenceValue(80f);

		assertEquals(71f, confidence.getAverage());
		assertEquals(62.2f, confidence.getMinimum());
		assertEquals(3, confidence.getNumberOfDataPoints());
		assertEquals(213f, confidence.getSum());

	}

	@Test
	void shouldWorkWithoutAddingValues() {

		assertEquals(0f, confidence.getAverage());
		assertEquals(0f, confidence.getMinimum());
		assertEquals(0, confidence.getNumberOfDataPoints());
		assertEquals(0f, confidence.getSum());
	}
}
