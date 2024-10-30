package org.jboss.sbomer.service.test.integ.rest;

import lombok.Getter;

public enum TestableApiVersion {
    V1ALPHA3("v1alpha3"), V1BETA1("v1beta1");

    @Getter
    String path;

    TestableApiVersion(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }
}