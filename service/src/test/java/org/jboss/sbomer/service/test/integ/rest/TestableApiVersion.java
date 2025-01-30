package org.jboss.sbomer.service.test.integ.rest;

import lombok.Getter;

public enum TestableApiVersion {
    V1ALPHA3("v1alpha3"), V1BETA1("v1beta1");

    @Getter
    final String name;

    TestableApiVersion(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String generationsPath() {
        switch (this) {
            case V1ALPHA3:
                return String.format("/api/%s/sboms/requests", name);
            case V1BETA1:
                return String.format("/api/%s/generations", name);
        }
        throw new RuntimeException("Unsupported API version");
    }

    public String manifestsPath() {
        switch (this) {
            case V1ALPHA3:
                return String.format("/api/%s/sboms", name);
            case V1BETA1:
                return String.format("/api/%s/manifests", name);
        }
        throw new RuntimeException("Unsupported API version");
    }
}
