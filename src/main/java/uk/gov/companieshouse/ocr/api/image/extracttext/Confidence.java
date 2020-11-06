package uk.gov.companieshouse.ocr.api.image.extracttext;

public class Confidence {
        private Float minimum = null;
        private int numberOfDataPoints = 0;
        private Float sum = 0f;

        public Float getAverage() {
            return sum / ((float) numberOfDataPoints);
        }

        public Float getMinimum() {
            return minimum;
        }

        public void setMinimum(Float minimum) {
            this.minimum = minimum;
        }

        public int getNumberOfDataPoints() {
            return numberOfDataPoints;
        }

        public void setNumberOfDataPoints(int numberOfDataPoints) {
            this.numberOfDataPoints = numberOfDataPoints;
        }

        public Float getSum() {
            return sum;
        }

        public void setSum(Float sum) {
            this.sum = sum;
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
            return "Confidence [average=" + getAverage() + ", minimum=" + minimum + ", numberOfDataPoints="
                    + numberOfDataPoints + ", sum=" + sum + "]";
        }

        
}
