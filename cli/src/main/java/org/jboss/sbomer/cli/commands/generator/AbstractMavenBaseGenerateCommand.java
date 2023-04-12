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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.client.SBOMerClient;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.cli.service.PNCService;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.utils.SbomUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractMavenBaseGenerateCommand implements Callable<Integer> {
    @Getter
    @ParentCommand
    MavenGenerateCommand parent;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CLI cli;

    @Inject
    PNCService pncService;

    @Inject
    @RestClient
    SBOMerClient sbomerClient;

    @Override
    public Integer call() throws Exception {
        // First, fetch the SBOM metadata
        Sbom sbom = sbomerClient.getById(parent.getParent().getSbomId());

        // Fetch build information
        Build build = pncService.getBuild(sbom.getBuildId());

        if (build == null) {
            log.error("Could not fetch the PNC build with id '{}'", parent.getParent().getSbomId());
            return CommandLine.ExitCode.SOFTWARE;
        }

        // Clone the source code related to the build
        doClone(build.getScmUrl(), build.getScmTag(), parent.getParent().getTargetDir(), parent.getParent().isForce());

        // Generate the SBOM
        Path bomPath = generate();

        log.info(
                "Preparing to upload SBOM for build '{}' from path '{}' to the service...",
                parent.getParent().getSbomId(),
                bomPath);

        log.debug("Reading generated SBOM from '{}' path", bomPath);

        // Read the file
        Bom bom = SbomUtils.fromPath(bomPath);

        if (bom == null) {
            throw new ApplicationException("Could parse the generated SBOM from '{}' path", bomPath.toString());
        }

        log.info("Uploading generated CycloneDX BOM...");

        sbomerClient.updateSbom(String.valueOf(sbom.getId()), SbomUtils.toJsonNode(bom));

        log.info("SBOM '{}' updated with generated BOM!", sbom.getId());

        return CommandLine.ExitCode.OK;
    }

    protected abstract GeneratorImplementation getGeneratorType();

    protected abstract Path generate();

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
}
