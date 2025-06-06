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
package org.jboss.sbomer.service.nextgen.resolver.advisory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.build.finder.koji.KojiClientSession;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.features.sbom.provider.KojiProvider;
import org.jboss.sbomer.core.rest.faulttolerance.RetryLogger;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.Build;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.nextgen.SBOMerClient;
import org.jboss.sbomer.service.nextgen.generator.GeneratorConfigProvider;
import org.jboss.sbomer.service.nextgen.resolver.AbstractResolver;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.ContextSpec;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsRequest;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationsResponse;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.TargetSpec;
import org.jboss.sbomer.service.rest.otel.TracingRestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiIdOrName;

import io.smallrye.faulttolerance.api.BeforeRetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@NoArgsConstructor
@Slf4j
public class ErrataToolAdvisoryResolver extends AbstractResolver {

    private static final int BATCH_SIZE = 50;
    public static final String RESOLVER_TYPE = "et-advisory";

    KojiProvider kojiProvider;
    KojiClientSession kojiSession;

    ErrataClient errataClient;

    SBOMerClient sbomerClient;

    GeneratorConfigProvider generatorConfigProvider;

    @Inject
    public ErrataToolAdvisoryResolver(
            ManagedExecutor managedExecutor,
            GeneratorConfigProvider generatorConfigProvider,
            @TracingRestClient ErrataClient errataClient,
            KojiProvider kojiProvider,
            @RestClient SBOMerClient sbomerClient) {
        super(managedExecutor);
        this.generatorConfigProvider = generatorConfigProvider;
        this.errataClient = errataClient;
        this.kojiProvider = kojiProvider;
        this.sbomerClient = sbomerClient;
    }

    @Override
    public String getType() {
        return RESOLVER_TYPE;
    }

    @Override
    public void resolve(String eventId, String advisoryId) {
        List<GenerationRequestSpec> generationRequests = resolveAdvisory(advisoryId);

        GenerationsRequest generationsRequest = new GenerationsRequest(
                null,
                new ContextSpec(eventId, "SBOMER", null, null),
                generationRequests);

        scheduleGenerations(eventId, generationsRequest);
    }

    void scheduleGenerations(String eventId, GenerationsRequest generationsRequest) {
        log.info("Requesting new generations...");

        // Event event = Event.findById(eventId);

        // if (event == null) {
        // log.warn("Event with id '{}' could not be found, cannot schedule generations", eventId);
        // return;
        // }

        GenerationsResponse generations = sbomerClient.requestGenerations(generationsRequest);

        // GenerationRequestSpec dummyRequestSpec = new GenerationRequestSpec(
        // new TargetSpec("quay.io/pct-security/mequal:latest", "CONTAINER_IMAGE"),
        // null);

        // GenerationRequestSpec effectiveRequest = generatorConfigProvider.buildEffectiveRequest(dummyRequestSpec);

        // // TODO: dummy
        // Generation generation = Generation.builder()
        // .withEvents(List.of(event))
        // // Convert payload to JsonNode
        // .withRequest(ObjectMapperUtils.toJsonNode(effectiveRequest))
        // .withReason("Generation created by Errata Tool resolver")
        // .build()
        // .save();

        // event.setStatus(EventStatus.RESOLVED);
        // event.setReason("Event was successfully resolved");
        // event.save();
    }

    public List<GenerationRequestSpec> resolveAdvisory(String advisoryId) {
        // Fetching Erratum
        Errata erratum = errataClient.getErratum(advisoryId);

        if (erratum == null) {
            throw new ClientException("Could not retrieve the Advisory '{}'", advisoryId);
        }

        Optional<Details> optDetails = erratum.getDetails();

        if (optDetails.isEmpty()) {
            throw new ClientException("Could not retrieve the Advisory '{}' details", advisoryId);
        }

        Details details = optDetails.get();

        // Will be removed, leave now for debugging
        printAllErratumData(erratum);

        if (!Boolean.TRUE.equals(details.getTextonly())) {
            System.out.println("IT IS A STANDARD ADVISORY");
            return handleStandardAdvisory(erratum);
        } else {
            System.out.println("IT IS A TEXT_ONLY ADVISORY");
            // return handleTextOnlyAdvisory(requestEvent, erratum);
        }

        return null;

    }

    private List<GenerationRequestSpec> handleStandardAdvisory(Errata erratum) {
        log.info(
                "Advisory {} ({}) is standard (non Text-Only), with status {}",
                erratum.getDetails().get().getFulladvisory(),
                erratum.getDetails().get().getId(),
                erratum.getDetails().get().getStatus());

        Details details = erratum.getDetails().get();
        if (details.getContentTypes().size() != 1) {

            String reason = String.format(
                    "The standard errata advisory has zero or multiple content-types (%s)",
                    details.getContentTypes());
            // doIgnoreRequest(requestEvent, reason);
        }

        if (details.getContentTypes().stream().noneMatch(type -> type.equals("docker") || type.equals("rpm"))) {
            String reason = String
                    .format("The standard errata advisory has unknown content-types (%s)", details.getContentTypes());
            // doIgnoreRequest(requestEvent, reason);
        }

        ErrataBuildList erratumBuildList = errataClient.getBuildsList(String.valueOf(details.getId()));
        Map<ProductVersionEntry, List<BuildItem>> buildDetails = erratumBuildList.getProductVersions()
                .values()
                .stream()
                .collect(
                        Collectors.toMap(
                                productVersionEntry -> productVersionEntry,
                                productVersionEntry -> productVersionEntry.getBuilds()
                                        .stream()
                                        .flatMap(build -> build.getBuildItems().values().stream())
                                        .toList()));

        // The are cases where an advisory might have no builds, let's ignore them to avoid a pending request
        if (buildDetails.values().stream().filter(Objects::nonNull).mapToInt(List::size).sum() == 0) {
            String reason = String.format("The standard errata advisory has no retrievable builds attached, skipping!");
            // doIgnoreRequest(requestEvent, reason);
        }

        // If the status is SHIPPED_LIVE and there is a successful generation for this advisory, create release
        // manifests.
        // Otherwise, SBOMer will default to the creation of build manifests.
        // This will change in the future!
        // V1Beta1RequestRecord successfulRequestRecord = null;
        // if (ErrataStatus.SHIPPED_LIVE.equals(details.getStatus())) {
        // log.debug(
        // "Errata status is SHIPPED_LIVE, looking for successful request records for advisory {}",
        // erratum.getDetails().get().getId());
        // successfulRequestRecord = sbomService.searchLastSuccessfulAdvisoryRequestRecord(
        // requestEvent.getId(),
        // String.valueOf(erratum.getDetails().get().getId()));
        // }

        // if (details.getContentTypes().contains("docker")) {
        // log.debug("Successful request records found: {}", successfulRequestRecord);

        // if (successfulRequestRecord == null) {
        // return createBuildManifestsForDockerBuilds(requestEvent, buildDetails);
        // } else {
        // return createReleaseManifestsForBuildsOfType(
        // erratum,
        // requestEvent,
        // buildDetails,
        // GenerationRequestType.CONTAINERIMAGE);
        // }

        // } else {
        // if (successfulRequestRecord == null) {
        // ErrataProduct product = errataClient.getProduct(details.getProduct().getShortName());
        // return createBuildManifestsForRPMBuilds(requestEvent, details, product, buildDetails);
        // } else {
        // return createReleaseManifestsForBuildsOfType(
        // erratum,
        // requestEvent,
        // buildDetails,
        // GenerationRequestType.BREW_RPM);
        // }
        // }

        List<GenerationRequestSpec> generationRequests = new ArrayList<>();

        List<Long> buildIds = buildDetails.values().stream().flatMap(List::stream).map(BuildItem::getId).toList();

        Map<Long, String> imageNamesFromBuilds;

        // Try to get the image names from builds, with retries handled in getImageNamesFromBuilds()
        try {
            imageNamesFromBuilds = getImageNamesFromBuilds(buildIds);
        } catch (KojiClientException e) {
            log.error("Failed to retrieve image names after retries", e);
            return null; // TODO
        }

        imageNamesFromBuilds.forEach((buildId, imageName) -> {
            log.debug("Retrieved imageName '{}' for buildId {}", imageName, buildId);
            if (imageName != null) {
                // SyftImageConfig config =
                // SyftImageConfig.builder().withIncludeRpms(true).withImage(imageName).build();
                log.debug("Creating GenerationRequest Kubernetes resource...");

                generationRequests.add(new GenerationRequestSpec(new TargetSpec(imageName, "CONTAINER_IMAGE"), null));

                // generations.add(
                // Generation.builder()
                // .withRequest(
                // ObjectMapperUtils.toJsonNode(
                // new Request(null, new Target(imageName, "CONTAINER_IMAGE"))))
                // .build());
                // sbomRequests.add(sbomService.generateSyftImage(requestEvent, config));
            }
        });

        return generationRequests;
    }

    // This method will be retried up to 10 times if a KojiClientException is thrown
    @Retry(maxRetries = 10, retryOn = KojiClientException.class)
    @BeforeRetry(RetryLogger.class)
    protected Map<Long, String> getImageNamesFromBuilds(List<Long> buildIds) throws KojiClientException {
        List<CompletableFuture<Map<Long, String>>> futures = new ArrayList<>();

        for (int i = 0; i < buildIds.size(); i += BATCH_SIZE) {
            List<Long> batch = buildIds.subList(i, Math.min(i + BATCH_SIZE, buildIds.size()));
            futures.add(CompletableFuture.supplyAsync(() -> fetchImageNames(batch), managedExecutor));
        }

        Map<Long, String> merged = new HashMap<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<Map<Long, String>> future : futures) {
                merged.putAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // Explicitly throw the exception again so that retry can happen
            log.error("Error while fetching image names for builds: {}", buildIds, e);
            kojiSession = null;
            throw new KojiClientException("Batch processing failed", e);
        }

        return merged;
    }

    @Retry(maxRetries = 10, retryOn = KojiClientException.class)
    @BeforeRetry(RetryLogger.class)
    protected KojiClientSession getKojiSession() throws KojiClientException {
        if (kojiSession == null) {
            kojiSession = kojiProvider.createSession();
        }
        return kojiSession;
    }

    protected Map<Long, String> fetchImageNames(List<Long> buildIds) {
        try {
            Map<Long, String> buildsToImageName = new HashMap<>();
            List<KojiIdOrName> ids = buildIds.stream().map(id -> new KojiIdOrName(id.intValue())).toList();

            List<KojiBuildInfo> buildInfos = getKojiSession().getBuild(ids);

            for (KojiBuildInfo info : buildInfos) {
                Map<String, Object> extra = info.getExtra();
                if (extra == null) {
                    continue;
                }

                Object imageObj = extra.get("image");
                if (!(imageObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> imageMap = (Map<String, Object>) imageObj;
                Object indexObj = imageMap.get("index");
                if (!(indexObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> indexMap = (Map<String, Object>) indexObj;
                Object pullsObj = indexMap.get("pull");
                if (!(pullsObj instanceof List)) {
                    continue;
                }

                List<?> pulls = (List<?>) pullsObj;
                if (pulls.isEmpty()) {
                    continue;
                }

                String imageName = pulls.stream()
                        .filter(item -> item instanceof String && ((String) item).contains("sha256"))
                        .map(Object::toString)
                        .findFirst()
                        .orElse(pulls.get(0).toString());

                buildsToImageName.put((long) info.getId(), imageName);
            }

            return buildsToImageName;
        } catch (KojiClientException e) {
            log.error("Unable to fetch containers information for buildIDs (batch): {}", buildIds, e);
            kojiSession = null;
            throw new RuntimeException(e);
        }
    }

    private void printAllErratumData(Errata erratum) {

        Optional<JsonNode> notes = erratum.getNotesMapping();

        if (notes.isEmpty()) {
            log.info("The erratum does not contain any JSON content inside the notes...");
        } else {
            log.info("The erratum contains a notes content with following JSON: \n{}", notes.get().toPrettyString());
        }

        if (erratum.getDetails().isEmpty()) {
            log.warn("Mmmmm I don't know how to get the release information...");
            return;
        }

        log.info("Fetching Erratum release ...");
        ErrataRelease erratumRelease = errataClient.getRelease(String.valueOf(erratum.getDetails().get().getGroupId()));

        log.info("Fetching Erratum builds list ...");
        ErrataBuildList erratumBuildList = errataClient
                .getBuildsList(String.valueOf(erratum.getDetails().get().getId()));

        StringBuilder summary = new StringBuilder("\n**********************************\n");
        summary.append("ID: ").append(erratum.getDetails().get().getId());
        summary.append("\nTYPE: ").append(erratum.getOriginalType());
        summary.append("\nAdvisory: ").append(erratum.getDetails().get().getFulladvisory());
        summary.append("\nSynopsis: ").append(erratum.getDetails().get().getSynopsis());
        summary.append("\nStatus: ").append(erratum.getDetails().get().getStatus());
        summary.append("\nCVE: ").append(erratum.getContent().getContent().getCve());
        summary.append("\n\nProduct: ")
                .append(erratum.getDetails().get().getProduct().getName())
                .append("(")
                .append(erratum.getDetails().get().getProduct().getShortName())
                .append(")");
        summary.append("\nRelease: ").append(erratumRelease.getData().getAttributes().getName());
        summary.append("\n\nBuilds: ");
        if (erratumBuildList != null && erratumBuildList.getProductVersions() != null
                && !erratumBuildList.getProductVersions().isEmpty()) {
            for (ProductVersionEntry productVersionEntry : erratumBuildList.getProductVersions().values()) {
                summary.append("\n\tProduct Version: ").append(productVersionEntry.getName());
                for (Build build : productVersionEntry.getBuilds()) {

                    summary.append("\n\t\t")
                            .append(
                                    build.getBuildItems()
                                            .values()
                                            .stream()
                                            .map(
                                                    buildItem -> "ID: " + buildItem.getId() + ", NVR: "
                                                            + buildItem.getNvr() + ", Variant: "
                                                            + buildItem.getVariantArch().keySet())
                                            .collect(Collectors.joining("\n\t\t")));
                }
            }
        }
        if (notes.isPresent()) {
            summary.append("\nJSON Notes:\n").append(notes.get().toPrettyString());
        } else {
            if (erratum.getContent().getContent().getNotes() != null
                    && !erratum.getContent().getContent().getNotes().trim().isEmpty()) {
                summary.append("\nNotes:\n").append(erratum.getContent().getContent().getNotes());
            }
        }

        summary.append("\n**********************************\n");
        System.out.println(summary); // NOSONAR: We want to use 'System.out' here
    }
}
