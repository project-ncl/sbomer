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
package org.jboss.sbomer.cli.feature.sbom.command.download;

import static org.jboss.sbomer.core.features.sbom.Constants.COM_REDHAT_COMPONENT;
import static org.jboss.sbomer.core.features.sbom.Constants.CONTAINER_PROPERTY_SYFT_PREFIX;
import static org.jboss.sbomer.core.features.sbom.Constants.IMAGE_LABELS;
import static org.jboss.sbomer.core.features.sbom.Constants.RELEASE;
import static org.jboss.sbomer.core.features.sbom.Constants.VERSION;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;

import jakarta.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        mixinStandardHelpOptions = true,
        name = "brew-sources",
        description = "Download the sources for a container image from Brew")
@Slf4j
@Setter
public class BrewSourcesDownloadCommand extends AbstractDownloadCommand {

    public static final String CONTAINER_PROPERTY_SYFT_IMAGE_LABELS_PREFIX = CONTAINER_PROPERTY_SYFT_PREFIX
            + IMAGE_LABELS;
    public static final String CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_COMPONENT = CONTAINER_PROPERTY_SYFT_IMAGE_LABELS_PREFIX
            + COM_REDHAT_COMPONENT;
    public static final String CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_VERSION = CONTAINER_PROPERTY_SYFT_IMAGE_LABELS_PREFIX
            + VERSION;
    public static final String CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_RELEASE = CONTAINER_PROPERTY_SYFT_IMAGE_LABELS_PREFIX
            + RELEASE;
    public static final String NVR_DELIMITER = "-";

    @Option(
            names = { "-p", "--path" },
            required = true,
            paramLabel = "FILE",
            description = "Location of the container image manifest file",
            converter = PathConverter.class)
    Path path;

    @Inject
    KojiService kojiService;

    @Override
    protected String getDownloaderType() {
        return "brew-sources";
    }

    @Override
    protected void doDownload(Path outputDir) {
        log.info("Reading BOM from {}", path.toAbsolutePath());
        Bom bom = SbomUtils.fromPath(path);
        String nvr = findNvr(bom);
        KojiBuildInfo buildInfo = findBuildInfo(nvr);
        if (buildInfo == null) {
            log.warn("No Brew build information was retrieved, unable to download remote sources");
            return;
        }
        kojiService.downloadSourcesFile(buildInfo, outputDir);
    }

    protected String findNvr(Bom bom) {
        // Try to find required properties
        List<Property> properties = bom.getMetadata().getProperties();
        Optional<Property> componentOpt = SbomUtils
                .findPropertyWithName(CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_COMPONENT, properties);
        Optional<Property> versionOpt = SbomUtils
                .findPropertyWithName(CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_VERSION, properties);
        Optional<Property> releaseOpt = SbomUtils
                .findPropertyWithName(CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_RELEASE, properties);
        List<String> missingProps = new ArrayList<>();
        if (componentOpt.isEmpty()) {
            missingProps.add(CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_COMPONENT);
        }
        if (versionOpt.isEmpty()) {
            missingProps.add(CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_VERSION);
        }
        if (releaseOpt.isEmpty()) {
            missingProps.add(CONTAINER_PROPERTY_SYFT_IMAGE_LABEL_RELEASE);
        }
        if (!missingProps.isEmpty()) {
            throw new ApplicationException(
                    "Missing required properties in main component: " + String.join(", ", missingProps)
                            + ". Unable to download sources for this container image");
        }
        return String.join(
                NVR_DELIMITER,
                componentOpt.get().getValue(),
                versionOpt.get().getValue(),
                releaseOpt.get().getValue());
    }

    private KojiBuildInfo findBuildInfo(String nvr) {
        log.debug("Looking up container information in Brew for NVR '{}'", nvr);
        try {
            return kojiService.findBuild(nvr);
        } catch (KojiClientException e) {
            throw new ApplicationException("Lookup in Brew failed", e);
        }
    }

}
