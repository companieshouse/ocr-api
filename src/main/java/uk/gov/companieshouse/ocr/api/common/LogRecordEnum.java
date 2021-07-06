package uk.gov.companieshouse.ocr.api.common;

/**
 *  This contains each unit Logging record that we will use for Log Analytics (e.g. CloudWatch)
 */
public enum LogRecordEnum implements LogFieldName, LogFieldValue {

    MONITORING_FIELDS("ocrMonitoring");

    private final String fieldValue;

    private LogRecordEnum(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    @Override
    public String getFieldName() {
        return "logRecordName";
    }

    @Override
    public String getFieldValue() {
        return fieldValue;
    }
    
}
