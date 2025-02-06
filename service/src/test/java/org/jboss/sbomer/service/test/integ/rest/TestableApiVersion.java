package org.jboss.sbomer.service.test.integ.rest;

import lombok.Getter;

@Getter
public enum TestableApiVersion {
    V1ALPHA3("v1alpha3"), V1BETA1("v1beta1");

    final String name;

    TestableApiVersion(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String generationsPath() {
        return switch (this) {
            case V1ALPHA3 -> String.format("/api/%s/sboms/requests", name);
            case V1BETA1 -> String.format("/api/%s/generations", name);
        };
    }

    public String manifestsPath() {
        return switch (this) {
            case V1ALPHA3 -> String.format("/api/%s/sboms", name);
            case V1BETA1 -> String.format("/api/%s/manifests", name);
        };
    }
}
