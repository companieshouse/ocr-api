package uk.gov.companieshouse.ocr.api.image.extracttext;

/**
 *  This represents the confidence that Tesseract has when it converts image to text on a set of data points (e.g. the whole document or a page).
 */
public class Confidence {
        private Float minimum = null;
        private int numberOfDataPoints = 0;
        private Float sum = 0f;

        public Float getAverage() {
            return numberOfDataPoints != 0 ? sum / numberOfDataPoints : 0.0f;
        }

        public Float getMinimum() {
            return minimum != null ? minimum : 0.0f;
        }

        public int getNumberOfDataPoints() {
            return numberOfDataPoints;
        }

        public Float getSum() {
            return sum;
        }

        public void addConfidenceValue(float confidenceValue) {
            numberOfDataPoints++;
            sum = Float.sum(sum, confidenceValue);

            if ((minimum == null) || (Float.compare(confidenceValue, minimum) < 0)) {
                minimum = confidenceValue;
            }
        }

        @Override
        public String toString() {
            return "Confidence [average=" + getAverage() + ", minimum=" + getMinimum() + ", numberOfDataPoints="
                    + numberOfDataPoints + ", sum=" + sum + "]";
        }
}
