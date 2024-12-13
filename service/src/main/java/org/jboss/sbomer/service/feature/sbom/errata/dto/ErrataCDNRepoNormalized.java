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

import java.util.Optional;

import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataCDNContentType;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataCDNReleaseType;

import lombok.Data;

@Data
public class ErrataCDNRepoNormalized {

    final String cdnName;
    final ErrataCDNReleaseType releaseType;
    final ErrataCDNContentType contentType;
    final long archId;
    final String archName;
    final String variantName;
    final int score;

    public ErrataCDNRepoNormalized(ErrataCDNRepo errataCDNRepo, String variantName, boolean stripName) {
        if (stripName) {
            this.cdnName = stripEndingSuffix(errataCDNRepo.getAttributes().getName());
        } else {
            // Keep the whole name (with '__{n}_DOT_{n}' suffix) for RHEL because some scanners use those versions to
            // figure out the different streams for RHEL
            this.cdnName = errataCDNRepo.getAttributes().getName();
        }
        this.releaseType = ErrataCDNReleaseType.fromName(errataCDNRepo.getAttributes().getReleaseType());
        this.contentType = ErrataCDNContentType.fromName(errataCDNRepo.getAttributes().getContentType());
        this.archId = errataCDNRepo.getRelationships().getArch().getId();
        this.archName = errataCDNRepo.getRelationships().getArch().getName();
        this.variantName = variantName;
        this.score = this.cdnName != null ? this.cdnName.length() : 0;
    }

    private static String stripEndingSuffix(String cdnName) {
        return Optional.ofNullable(cdnName).map(name -> {
            int lastSlashIndex = name.lastIndexOf("__");
            return lastSlashIndex != -1 ? name.substring(0, lastSlashIndex) : name;
        }).orElse(null);
    }

}
