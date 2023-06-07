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
package org.jboss.sbomer.cli.commands.generator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

import org.cyclonedx.model.Bom;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.jboss.sbomer.cli.commands.AbstractCommand;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.service.rest.Page;
import org.jboss.sbomer.core.utils.MDCUtils;
import org.jboss.sbomer.core.utils.SbomUtils;
import org.jboss.sbomer.core.utils.maven.MavenCommandLineParser;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractMavenGenerateCommand extends AbstractCommand {
    // @Getter
    @ParentCommand
    MavenGenerateCommand parent;

    @Override
    public Integer call() throws Exception {

        try {
            String sbomId = parent.getParent().getSbomMixin().getSbomId();

            // make sure there is no context
            MDCUtils.removeContext();
            MDCUtils.addProcessContext(sbomId);

            // First, fetch the SBOM metadata
            Sbom sbom = sbomerClient.getById(sbomId, sbomId);

            // make sure there is no context
            MDCUtils.addBuildContext(sbom.getBuildId());

            log.info("Starting generation for PNC Build '{}'", sbom.getBuildId());

            // Fetch build information
            Build build = pncService.getBuild(sbom.getBuildId());

            if (build == null) {
                log.error("Could not fetch the PNC build with id '{}'", sbom.getBuildId());
                return CommandLine.ExitCode.SOFTWARE;
            }

            // Filter only valid Pnc builds
            if (!isValidBuild(build)) {
                log.error(
                        "Build is not valid! Needs to be a FINISHED build of type MVN with status SUCCESS or NO_REBUILD_REQUIRED");
                return CommandLine.ExitCode.SOFTWARE;
            }

            // Get the correct scm information for builds which have either SUCCESS or NO_REBUILD_REQUIRED status
            String scmUrl = build.getScmUrl();
            String scmTag = build.getScmTag();
            String originalBuildId = null;
            Bom bom = null;
            if (org.jboss.pnc.enums.BuildStatus.NO_REBUILD_REQUIRED.equals(build.getStatus())) {
                scmUrl = build.getNoRebuildCause().getScmUrl();
                scmTag = build.getNoRebuildCause().getScmTag();
                originalBuildId = build.getNoRebuildCause().getId();

                PaginationParameters pagParams = new PaginationParameters();
                pagParams.setPageIndex(0);
                pagParams.setPageSize(1);
                String rsqlQuery = "buildId=eq=" + originalBuildId + ";generator=isnull=false;processors=isnull=true";

                log.info("Searching SBOMs with rsqlQuery: {}", rsqlQuery);

                Page<Sbom> sboms = sbomerClient.searchSboms(String.valueOf(sbom.getId()), pagParams, rsqlQuery);
                if (sboms.getContent().size() > 0) {
                    bom = SbomUtils.fromJsonNode(sboms.getContent().iterator().next().getSbom());
                } else {
                    log.warn(
                            "Could not find original SBOM for PNC build '{}', will regenerate the SBOM...",
                            originalBuildId);
                }
            }

            if (bom == null) {

                // Clone the source code related to the build
                doClone(scmUrl, scmTag, parent.getParent().getTargetDir(), parent.getParent().isForce());

                // In case the original build command script contains profiles, projects list or system properties
                // definitions,
                // get them as a best effort and pass them to the SBOM generation to try to resolve the same dependency
                // tree.
                String buildCmdOptions = "mvn";
                try {
                    MavenCommandLineParser lineParser = MavenCommandLineParser.build()
                            .launder(build.getBuildConfigRevision().getBuildScript());
                    buildCmdOptions = lineParser.getRebuiltMvnCommandScript();
                } catch (IllegalArgumentException exc) {
                    log.error(
                            "Could not launder the provided build command script! Using the default build command",
                            exc);
                }

                // Generate the SBOM
                Path bomPath = generate(buildCmdOptions);

                log.info(
                        "Preparing to update SBOM id: '{}' (PNC build '{}') with generated SBOM content from path '{}'...",
                        sbom.getId(),
                        sbom.getBuildId(),
                        bomPath);

                log.debug("Reading generated SBOM from '{}' path", bomPath);

                // Read the file
                bom = SbomUtils.fromPath(bomPath);

                if (bom == null) {
                    throw new ApplicationException("Could parse the generated SBOM from '{}' path", bomPath.toString());
                }
            }

            log.info("Uploading generated CycloneDX BOM...");

            sbomerClient
                    .updateSbom(String.valueOf(sbom.getId()), String.valueOf(sbom.getId()), SbomUtils.toJsonNode(bom));

            log.info("SBOM '{}' updated with generated BOM!", sbom.getId());

            return CommandLine.ExitCode.OK;
        } finally {
            MDCUtils.removeContext();
        }
    }

    protected abstract GeneratorImplementation getGeneratorType();

    protected abstract Path generate(String buildCmdOptions);

    protected void doClone(String url, String tag, Path path, boolean force) {
        log.info("Cloning '{}' repository and '{}' tag...", url, tag);

        if (Files.exists(path)) {
            if (!force) {
                throw new ApplicationException("Path '{}' already exists and force is disabled", path);
            }

            log.warn("Cleaning up the '{}' output directory before cloning...", path);

            // Delete the output directory directory
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("Could not delete the '{}' directory", path, e);
            }

            log.debug("Done.");
        }

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Could not create the '{}' directory", path, e);
        }

        log.info("Cloning the repository...", path);
        try {
            Git.cloneRepository().setDirectory(path.toFile()).setURI(url).setBranch(tag).setDepth(1).call();
        } catch (InvalidRemoteException e) {
            throw new ApplicationException("Unknown error occurred while preparing to clone the repository", e);
        } catch (GitAPIException e) {
            log.error("Unable to clone the repository", e);
            throw new ApplicationException("Unable to clone the repository", e);
        }

        log.info("Done, source code available in the '{}' directory", path);

        // Please note that this ignores the block size of a particular storage,
        // and because of this, the number can be different compared to when
        // an utility like `du` is being used to determine the size.
        log.info("Directory size: {} MB", String.format("%.02f", Float.valueOf(dirSize(path)) / 1024));
    }

    /**
     * Calculates directory size for a given path.
     *
     * @param path
     * @return Size in kB.
     */
    protected long dirSize(Path path) {
        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("An error occurred while calculating size of the directory");
        }

        return size.get() / 1024;
    }

    private boolean isValidBuild(Build build) {
        if (!build.getTemporaryBuild() && org.jboss.pnc.enums.BuildProgress.FINISHED.equals(build.getProgress())
                && (org.jboss.pnc.enums.BuildStatus.SUCCESS.equals(build.getStatus())
                        || org.jboss.pnc.enums.BuildStatus.NO_REBUILD_REQUIRED.equals(build.getStatus()))
                && org.jboss.pnc.enums.BuildType.MVN.equals(build.getBuildConfigRevision().getBuildType())) {
            return true;
        }
        return false;
    }
}
