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
package org.jboss.sbomer.service.test.integ.feature.s3;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.s3.S3ClientFacade;
import org.jboss.sbomer.service.feature.s3.S3StorageHandler;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.test.integ.feature.s3.S3FeatureIT.S3ClientConfig;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(S3ClientConfig.class)
class S3FeatureIT {

    @InjectSpy
    S3StorageHandler storageHandler;

    @InjectMock
    GenerationRequestControllerConfig controllerConfig;

    @InjectSpy
    FeatureFlags featureFlags;

    @InjectMock
    S3ClientFacade clientFacade;

    public static class S3ClientConfig extends TestUmbProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "bucket.region",
                    "us-east-1",
                    "bucket.name",
                    "bucket-name",
                    "aws.access.key.id",
                    "access-key",
                    "aws.secret.access.key",
                    "secret-access-key",
                    "sbomer.features.umb.enabled",
                    "true");
        }
    }

    @BeforeAll
    public static void init() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

    }

    @Test
    void testStoreFiles(@TempDir Path tempDir) throws IOException {
        when(controllerConfig.sbomDir()).thenReturn(tempDir.toAbsolutePath().toString());

        ObjectMeta meta = mock(ObjectMeta.class);
        when(meta.getName()).thenReturn("sbom-request-123");

        GenerationRequest generationRequest = mock(GenerationRequest.class);
        when(generationRequest.getMetadata()).thenReturn(meta);
        when(generationRequest.getId()).thenReturn("AABBCC");

        Path generationDir = tempDir.resolve("sbom-request-123");

        // Create the directory
        generationDir.toFile().mkdirs();

        // Place a file inside the dir
        Path file = generationDir.resolve("bom.json");
        Files.write(file, "{}".getBytes());

        // Add some logs
        Path logsDir = generationDir.resolve("logs");
        logsDir.toFile().mkdirs();

        Path logFile = logsDir.resolve("init.log");
        Files.write(logFile, "Some log".getBytes());

        // doNothing().when(clientFacade).upload(file.toFile().getAbsolutePath(),
        // "bucket-name/bucket-name/bom.json");
        doNothing().when(clientFacade).upload(eq(file.toFile().getAbsolutePath()), eq("AABBCC/bom.json"));
        doNothing().when(clientFacade).upload(eq(logFile.toFile().getAbsolutePath()), eq("AABBCC/logs/init.log"));

        storageHandler.storeFiles(generationRequest);
    }

    @Test
    void testDisabledS3Endpoint() {
        when(featureFlags.s3Storage()).thenReturn(false);

        RestAssured.given()
                .when()
                .get("/api/v1beta1/generations/NOPE/logs")
                .then()
                .statusCode(503)
                .body("resource", CoreMatchers.is("/api/v1beta1/generations/NOPE/logs"))
                .body("errorId", CoreMatchers.isA(String.class))
                .body("message", CoreMatchers.is("S3 feature is disabled currently, try again later"))
                .body("$", Matchers.not(Matchers.hasKey("errors")));
    }

    @Test
    void testNotFoundS2LogBecuaseGenerationDoesNotExist() {
        when(featureFlags.s3Storage()).thenReturn(true);

        RestAssured.given()
                .when()
                .get("/api/v1beta1/generations/NOPE/logs")
                .then()
                .statusCode(404)
                .body("resource", CoreMatchers.is("/api/v1beta1/generations/NOPE/logs"))
                .body("errorId", CoreMatchers.isA(String.class))
                .body("message", CoreMatchers.is("GenerationRequest with id 'NOPE' could not be found"))
                .body("$", Matchers.not(Matchers.hasKey("errors")));
    }

    @Test
    void testReturnLogPaths() {
        SbomGenerationRequest request = new SbomGenerationRequest();
        request.setStatus(SbomGenerationStatus.FINISHED);

        PanacheMock.mock(SbomGenerationRequest.class);
        when(SbomGenerationRequest.findById("REQUESTID")).thenReturn(request);

        when(clientFacade.logFileNames("REQUESTID"))
                .thenReturn(List.of("a/path/to/generate.log", "a/path/to/init.log"));

        // Ensure s3 feature is enabled
        when(featureFlags.s3Storage()).thenReturn(true);

        RestAssured.given()
                .when()
                .get("/api/v1beta1/generations/REQUESTID/logs")
                .then()
                .assertThat()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("$", Matchers.hasItems("a/path/to/generate.log", "a/path/to/init.log"));
    }

    @Test
    void testFetchLogByPath() {
        SbomGenerationRequest request = new SbomGenerationRequest();
        request.setStatus(SbomGenerationStatus.FINISHED);

        PanacheMock.mock(SbomGenerationRequest.class);
        when(SbomGenerationRequest.findById("REQUESTID")).thenReturn(request);

        when(clientFacade.log("REQUESTID", "a/path/to/generate.log")).thenReturn("This is a log content");

        // Ensure s3 feature is enabled
        when(featureFlags.s3Storage()).thenReturn(true);

        RestAssured.given()
                .accept(ContentType.TEXT)
                .when()
                .get("/api/v1beta1/generations/REQUESTID/logs/{path}", "a/path/to/generate.log")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(Matchers.equalTo("This is a log content"));
    }
}
