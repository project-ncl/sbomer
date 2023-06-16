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
package org.jboss.sbomer.feature.sbom.cli.command;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.service.PncService;
import org.jboss.sbomer.core.utils.MDCUtils;
import org.jboss.sbomer.feature.sbom.cli.command.mixin.GeneratorToolMixin;
import org.jboss.sbomer.feature.sbom.core.config.DefaultGenerationConfig;
import org.jboss.sbomer.feature.sbom.core.enums.GeneratorType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractGenerateCommand implements Callable<Integer> {
    @Mixin
    GeneratorToolMixin generator;

    @Getter
    @ParentCommand
    GenerateCommand parent;

    @Inject
    protected PncService pncService;

    @Inject
    DefaultGenerationConfig defaultGenerationConfig;

    /**
     * <p>
     * Implementation of the SBOM generation for project located in the {@code parent.getOutput()} directory.
     * </p>
     *
     * @return a {@link Path} to the generated BOM file.
     */
    protected abstract Path doGenerate();

    @Override
    public Integer call() throws Exception {
        // Make sure there is no context
        MDCUtils.removeContext();
        MDCUtils.addBuildContext(parent.getBuildId());

        // Fetch build information
        Build build = pncService.getBuild(parent.getBuildId());

        if (build == null) {
            log.error("Could not fetch the PNC build with id '{}'", parent.getBuildId());
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

        // Clone the source code related to the build
        doClone(scmUrl, scmTag, parent.getWorkdir(), parent.isForce());

        // Generate the SBOM
        Path sbomPath = doGenerate();

        try {
            Files.copy(sbomPath, parent.getOutput(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApplicationException(
                    "Could not move the generated SBOM to target location: '{}'",
                    parent.getOutput().toAbsolutePath());
        }

        log.info("Generation finished, SBOM available at: '{}'", parent.getOutput().toFile().getAbsolutePath());
        return 0;
    }

    String toolVersion(GeneratorType type) {
        String toolVersion = generator.getVersion();

        if (toolVersion == null) {
            toolVersion = defaultGenerationConfig.forGenerator(type).defaultVersion();
            log.debug("Using default tool version for the {} generator: {}", type, toolVersion);
        } else {
            log.debug("Using provided version for the {} generator: {}", type, toolVersion);
        }

        return toolVersion;
    }

    protected void doClone(String url, String tag, Path path, boolean force) {
        log.info("Cloning '{}' repository and '{}' tag into '{}'...", url, tag, path.toAbsolutePath());

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
