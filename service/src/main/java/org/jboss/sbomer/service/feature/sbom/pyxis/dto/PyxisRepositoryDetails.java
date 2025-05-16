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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PyxisRepositoryDetails {

    // data=[] should give a constraint error
    @NotEmpty(message = "Pyxis registry looks to be empty")
    private List<DataSection> data;

    //TIL AssertTrue expects method must start with is, get or has
    @AssertTrue(message = "At least one Pyxis repository must be published")
    public boolean isOneRepositoryPublished() {
        for (DataSection dataSection : this.data) {
            if (dataSection != null && dataSection.getRepositories() != null) {
                for (Repository repository : dataSection.getRepositories()) {
                    if (repository != null && repository.isPublished()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Data
    public static class DataSection {
        private List<Repository> repositories;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        @AssertTrue
        private boolean published;

        private String registry;

        private String repository;

        private List<Tag> tags;

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        public static class Tag {
            private String name;
        }
    }
}
