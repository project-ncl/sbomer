/*
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

package org.jboss.sbomer.service.feature.s3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Sbom.GenerationRequest;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@ApplicationScoped
@Slf4j
public class S3ClientFacade {

    private S3Client client;

    @Inject
    FeatureFlags featureFlags;

    String bucketName() {
        return ConfigProvider.getConfig().getValue("bucket.name", String.class);
    }

    String bucketRegion() {
        return ConfigProvider.getConfig().getValue("bucket.region", String.class);
    }

    void init(@Observes StartupEvent ev) {
        if (featureFlags.s3Storage()) {
            ensureClient();
        }
    }

    /**
     * Event listener for the {@link FeatureFlags.TOGGLE_S3_STORAGE} toggle. In case it is enabled, the S3 client is
     * instantiated. When the toggle is disabled, client is closed.
     *
     * @param flag
     */
    @ConsumeEvent(FeatureFlags.EVENT_NAME)
    void featureFlag(Map<String, Boolean> flag) {
        Boolean s3Feature = flag.get(FeatureFlags.TOGGLE_S3_STORAGE);

        if (s3Feature == null) {
            return;
        }

        // S3 storage feature flag was changed.
        if (s3Feature) {
            // And it was enabled.

            log.debug("Enabling S3 storage handler...");

            ensureClient();
        } else {
            // And it was disabled, close the client, if there is one.
            if (client != null) {
                log.debug("Disabling S3 storage handler");
                client.close();
                client = null;
            }
        }
    }

    /**
     * Ensure that we have a S3 client initialized and ready. Validates configuration required to instantiate the
     * client.
     */
    public void ensureClient() {
        if (client != null) {
            // In case the client it available, will assume it's valid.
            return;
        }

        validateClientConfiguration();

        log.debug("Instantiating new S3 client");

        client = S3Client.builder().region(Region.of(bucketRegion())).build();

        log.info("S3 client instantiated");
    }

    /**
     * <p>
     * Validates configuration (env variables) so that the S3 client can be instantiated.
     * </p>
     *
     * <p>
     * It uses properties, but these are internally translated into env variables, for example {@code bucket.name}
     * becomes {@code BUCKET_NAME} env variable.
     * </p>
     *
     * <p>
     * The {@code AWS_ACCESS_KEY_ID} and {@code AWS_SECRET_ACCESS_KEY} env variables are handled by the client
     * internally. Others are used directly in this helper class.
     * </p>
     */
    private void validateClientConfiguration() {
        log.debug("Validating S3 client configuration...");

        Set<String> missingConfig = new HashSet<>();

        for (String prop : Set.of("bucket.region", "bucket.name", "aws.access.key.id", "aws.secret.access.key")) {
            Optional<String> valueOpt = ConfigProvider.getConfig().getOptionalValue(prop, String.class);

            if (valueOpt.isEmpty()) {
                missingConfig.add(prop.toUpperCase().replace('.', '_'));
            }
        }

        if (!missingConfig.isEmpty()) {
            throw new ApplicationException(
                    "S3 client cannot be instantiated, following env variables are missing: {}",
                    missingConfig);
        }

        log.debug("S3 client configuration is valid");
    }

    public boolean doesObjectExists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName()).key(key).build();
            client.headObject(request);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            } else {
                throw new ApplicationException("An error occurred when checking if object '{}' exists in S3", key, e);
            }
        } catch (SdkException e) {
            throw new ApplicationException("An error occurred when checking if object '{}' exists in S3", key, e);
        }
    }

    public void upload(String path, String key) {
        log.debug("Uploading '{}' file as '{}'...", path, key);

        try {
            PutObjectRequest request = PutObjectRequest.builder().key(key).bucket(bucketName()).build();
            client.putObject(request, Path.of(path));
        } catch (SdkException e) {
            throw new ApplicationException("An error occurred when uploading '{}' file to S3", path, e);
        }
    }

    /**
     * Returns list of paths within the S3 bucket to log files for a given {@link GenerationRequest} identifier.
     *
     * @param generationRequestId
     * @return
     */
    public List<String> logFileNames(String generationRequestId) {
        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(bucketName())
                .prefix(generationRequestId)
                .build();

        ListObjectsV2Response objects = client.listObjectsV2(req);

        List<String> paths = new ArrayList<>();

        for (S3Object object : objects.contents()) {
            log.debug("Found: '{}'", object.key());

            if (object.key().contains("/logs/")) {
                paths.add(object.key().replace(generationRequestId + "/", ""));
            }
        }

        return paths;
    }

    /**
     * Returns list of paths within the S3 bucket to log files for a given {@link GenerationRequest} identifier.
     *
     * @param generationRequestId
     * @return
     */
    public String log(String generationRequestId, String path) {

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucketName())
                .key(generationRequestId + "/" + path)
                .build();

        return client.getObjectAsBytes(req).asUtf8String();
    }
}
