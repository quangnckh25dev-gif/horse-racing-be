package com.horseracing.dto;

public class OptionResponse {
    private String value;
    private String label;

    public OptionResponse(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
