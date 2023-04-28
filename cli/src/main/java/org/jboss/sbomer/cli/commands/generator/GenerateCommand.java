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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.jboss.sbomer.cli.commands.AbstractCommand;
import org.jboss.sbomer.cli.commands.mixins.SbomMixin;
import org.jboss.sbomer.cli.converters.PathConverter;
import org.jboss.sbomer.core.errors.ApplicationException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "generate",
        aliases = { "g" },
        description = "Generate SBOM from source code",
        subcommands = { MavenGenerateCommand.class })
public class GenerateCommand extends AbstractCommand {
    @Mixin
    @Getter
    SbomMixin sbomMixin;

    @Getter
    @Option(
            names = { "--force" },
            description = "If the target directory (specified with --dir) should be cleaned up in case it already exists. Please note that in case --force is used, the path will be removed without questions! Default: ${DEFAULT-VALUE}",
            scope = ScopeType.INHERIT)
    boolean force = false;

    @Getter
    @Option(
            names = { "--target" },
            defaultValue = "target",
            paramLabel = "DIR",
            description = "The target directory where the source code should checked out. Default: ${DEFAULT-VALUE}",
            converter = PathConverter.class,
            scope = ScopeType.INHERIT)
    Path targetDir;

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
