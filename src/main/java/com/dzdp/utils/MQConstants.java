package com.dzdp.utils;

public enum MQConstants {

    secKillTopic("secKill");

    private String value;

    MQConstants(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
