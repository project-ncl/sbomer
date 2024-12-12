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
package org.jboss.sbomer.service.feature.sbom.pyxis.dto;

import java.util.Optional;

import lombok.Data;

@Data
public class RepositoryCoordinates {

    final String registry;
    final String repository;
    final String repositoryFragment;
    final String tag;
    final int score;

    public RepositoryCoordinates(String registry, String repository, String tag) {
        this.registry = registry;
        this.repository = repository;
        this.tag = tag;
        // Extract last fragment from the repository
        this.repositoryFragment = Optional.ofNullable(repository).map(repo -> {
            int lastSlashIndex = repo.lastIndexOf('/');
            return lastSlashIndex != -1 ? repo.substring(lastSlashIndex + 1) : repo;
        }).orElse(null);
        this.score = (this.repositoryFragment != null ? this.repositoryFragment.length() : 0)
                + (this.tag != null ? this.tag.length() : 0);
    }

}
