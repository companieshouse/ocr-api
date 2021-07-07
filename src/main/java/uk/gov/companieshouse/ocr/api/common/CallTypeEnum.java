package uk.gov.companieshouse.ocr.api.common;

public enum CallTypeEnum implements LogFieldName,  LogFieldValue {

    ASYNCHRONOUS("asynchronous"),
    SYNCHRONOUS("synchronous");

    private final String callTypeValue;

    private CallTypeEnum(String callTypeValue) {
        this.callTypeValue = callTypeValue;
    }

    @Override
    public String getFieldName() {
        return "callType";
    }

    @Override
    public String getFieldValue() {
        return callTypeValue;
    }
}
