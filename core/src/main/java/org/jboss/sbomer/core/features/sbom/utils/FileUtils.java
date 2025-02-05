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
package org.jboss.sbomer.core.features.sbom.utils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {

    public static final String MANIFEST_FILENAME = "bom.json";

    private FileUtils() {
        // this is a utlity class
    }

    public static void rmdir(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
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
            log.error("Could not delete the '{}' directory", dir, e);
        }
    }

    /**
     * Traverses through the directory tree and finds manifest (files that have {@code bom.json}) and returns all found
     * files as a {@link List} of {@link Path}s.
     *
     * @param directory The top-level directory where search for manifests should be started.
     * @return List of {@link Path}s to found manifests.
     */
    public static List<Path> findManifests(Path directory) throws IOException {
        // Check if directory exists and is a directory
        if (Files.notExists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "The provided path is not a valid directory: " + directory.toAbsolutePath());
        }

        log.info("Finding manifests under the '{}' directory...", directory.toAbsolutePath());

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> manifestPaths = paths.filter(path -> MANIFEST_FILENAME.equals(path.getFileName().toString()))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .peek(path -> log.info("Found manifest at path '{}'", path.toAbsolutePath()))
                    .toList();

            log.info("Found {} generated manifests", manifestPaths.size());
            return manifestPaths;
        }
    }

}
