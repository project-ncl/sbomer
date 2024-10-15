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
package org.jboss.sbomer.service.feature.sbom.errata.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

@Data
public class ErrataBuildList {

    private Map<String, ProductVersionEntry> productVersions = new HashMap<String, ProductVersionEntry>();

    @JsonAnySetter
    public void addProductVersion(String name, ProductVersionEntry productVersionEntry) {
        this.productVersions.put(name, productVersionEntry);
    }

    @Data
    public static class ProductVersionEntry {
        private String name;
        private String description;
        private List<Build> builds = new ArrayList<Build>();
    }

    @Data
    public static class Build {
        private Map<String, BuildItem> buildItems = new HashMap<String, BuildItem>();

        @JsonAnySetter
        public void addBuildItem(String name, BuildItem buildItem) {
            this.buildItems.put(name, buildItem);
        }
    }

    @Data
    public static class BuildItem {
        private String nvr;
        private String nevr;
        private Long id;
    }
}