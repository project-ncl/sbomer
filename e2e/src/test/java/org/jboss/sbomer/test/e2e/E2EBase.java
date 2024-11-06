/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.test.e2e;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.awaitility.Awaitility;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class E2EBase {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenerationRequest {
        String id;
        String type;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sbom {
        GenerationRequest generationRequest;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        Sbom sbom;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawMessage {
        Map<String, String> headers;
        Message msg;
    }

    public abstract String datagrepperUriPropertyName();

    public abstract String sbomerUriPropertyName();

    protected String getSbomerBaseUri() {
        return validateProperty(sbomerUriPropertyName());
    }

    protected String getDatagrepperBaseUri() {
        return validateProperty(datagrepperUriPropertyName());
    }

    private String validateProperty(String propertyName) {
        String uri = System.getProperty(propertyName);

        if (uri == null) {
            throw new RuntimeException(
                    String.format(
                            "Could not obtain required property, make sure you set the '-D%s' property",
                            propertyName));
        }

        return uri;
    }

    protected void waitForGeneration(String generationRequestId) {
        waitForGeneration(generationRequestId, 20, TimeUnit.MINUTES);
    }

    protected void waitForGeneration(String generationId, long time, TimeUnit unit) {
        Awaitility.await().atMost(time, unit).pollInterval(10, TimeUnit.SECONDS).until(() -> {
            final Response response = getGeneration(generationId);

            final String status = response.body().jsonPath().getString("status");

            log.info(
                    "Generation '{}' (type: '{}', identifier: '{}') current status: {}",
                    generationId,
                    response.body().jsonPath().getString("type"),
                    response.body().jsonPath().getString("identifier"),
                    status);

            if (status.equals("FAILED")) {
                log.error("Generation '{}' failed: {}", generationId, response.asPrettyString());
                throw new Exception(String.format("GenerationRequest '%s' failed, see logs above", generationId));
            }

            return status.equals("FINISHED");
        });

        log.info("Generation '{}' successfully finished", generationId);
    }

    public void publishedUmbMessage(String generationRequestId, Consumer<ValidatableResponse> consumer) {
        AtomicReference<ValidatableResponse> atomicResponse = new AtomicReference<>();

        Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {

            log.info("Finding last UMB message available for GenerationRquest: '{}'", generationRequestId);

            Response response = RestAssured.given()
                    .baseUri(getDatagrepperBaseUri())
                    .param("delta", "43200") // 12 hours in seconds
                    .param("topic", "/topic/VirtualTopic.eng.pnc.sbom.complete")
                    .param("order", "desc")
                    .get("/raw");

            if (response.body().jsonPath().getInt("count") == 0) {
                log.debug("No UMB messages found");
                return false;
            }

            List<RawMessage> rawMessages = response.body().jsonPath().getList("raw_messages", RawMessage.class);

            OptionalInt indexOpt = IntStream.range(0, rawMessages.size())
                    .filter(
                            index -> rawMessages.get(index)
                                    .getMsg()
                                    .getSbom()
                                    .getGenerationRequest()
                                    .getId()
                                    .equals(generationRequestId))
                    .findFirst();

            if (indexOpt.isEmpty()) {
                log.debug("No UMB messages found for GenerationRquest '{}'", generationRequestId);
                return false;
            }

            log.info(
                    "Message with ID '{}' found for GenerationRquest: '{}'!",
                    response.body().jsonPath().getString("msg_id"),
                    generationRequestId);

            atomicResponse.set(response.then().rootPath("raw_messages[" + indexOpt.getAsInt() + "]"));

            return true;
        });

        consumer.accept(atomicResponse.get());
    }

    public Response getGeneration(String generationId) {
        log.info("Fetching generation with id '{}'", generationId);

        Response response = RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("/api/v1beta1/generations/%s", generationId));

        log.info("Got: {}", response.body().asPrettyString());

        return response;
    }

    public Response getManifestsForGeneration(String generationId) {
        log.info("Fetching manifests for generation with id '{}'", generationId);

        Response response = RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("/api/v1beta1/manifests?query=generation.id==%s", generationId));

        log.info("Got: {}", response.body().asPrettyString());

        return response;
    }

    public List<String> requestGeneration(String jsonBody) {
        log.info("Requesting generation of manifest with jsonBody: {}", jsonBody);

        Response response = RestAssured.given()
                .baseUri(getSbomerBaseUri())
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .contentType(ContentType.JSON)
                .post("/api/v1beta1/generations");

        response.then().statusCode(202);

        log.info("Got: {}", response.body().asPrettyString());

        return response.jsonPath().getList("id");
    }
}
