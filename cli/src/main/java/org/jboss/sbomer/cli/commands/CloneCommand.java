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
package org.jboss.sbomer.cli.commands;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.service.PNCService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command to clone a repository to obtain source code related to a particular PNC build ID.
 *
 */
@Slf4j
@Command(mixinStandardHelpOptions = true, name = "clone", aliases = { "c" }, description = "Clone repository")
public class CloneCommand implements Runnable {

    @Inject
    CLI cli;

    @Inject
    PNCService pncService;

    @Option(
            names = { "-b", "--build-id" },
            required = true,
            description = "The PNC build id for which we should fetch the source code, example: AABBCCDD")
    @Getter
    String buildId;

    @Option(
            names = { "--force" },
            description = "If the output directory should be cleaned up in case it already exists. Please note that in case --force is used, the path will be removed without questions!")
    boolean force = false;

    @Option(
            names = { "-d", "--directory" },
            defaultValue = "source",
            paramLabel = "DIR",
            description = "The target directory where the source code should checked out")
    Path output;

    @Override
    public void run() {
        log.info("Fetching build information for PNC Build '{}'", buildId);

        Build build = pncService.getBuild(buildId);

        log.debug("Build information fetched");

        log.info("Cloning '{}' repository and '{}' tag...", build.getScmUrl(), build.getScmTag());

        if (Files.exists(output)) {
            if (!force) {
                log.warn("Path '{}' already exists and the --force parameter is not specified, exiting", output);
                System.exit(-1);
            }

            log.debug("Cleaning up the '{}' output directory before cloning...", output);

            // Delete the output directory directory
            try {
                Files.walkFileTree(output, new SimpleFileVisitor<Path>() {
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
                log.error("Could not delete the '{}' directory", output, e);
            }

            log.debug("Done.");
        }

        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            log.error("Could not create the '{}' directory", output, e);
        }

        log.info("Cloning the repository...", output);
        try {
            Git.cloneRepository()
                    .setDirectory(output.toFile())
                    .setURI(build.getScmUrl())
                    .setBranch(build.getScmTag())
                    .setDepth(1)
                    .call();
        } catch (InvalidRemoteException e) {
            log.error("Unknown error occurred while preparing to clone the repository", e);
            System.exit(-1);
        } catch (GitAPIException e) {
            log.error("Unable to clone the repository", e);
            System.exit(-1);
        }

        log.info("Done, source code available in the '{}' directory", output);

        // Please note that this ignores the block size of a particular storage,
        // and because of this, the number can be different compared to when
        // an utility like `du` is being used to determine the size.
        log.info("Directory size: {} MB", String.format("%.02f", Float.valueOf(size(output)) / 1024 / 1024));
    }

    private long size(Path path) {
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
            throw new AssertionError("An error occurred");
        }

        return size.get();
    }

}
