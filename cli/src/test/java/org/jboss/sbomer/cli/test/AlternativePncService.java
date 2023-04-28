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
package org.jboss.sbomer.cli.test;

import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.sbomer.core.service.PncService;

@Alternative
@Singleton
public class AlternativePncService extends PncService {

    @Override
    public Build getBuild(String buildId) {
        return Build.builder()
                .id("BBVVCC")
                .environment(
                        Environment.builder()
                                .systemImageId("imageid")
                                .systemImageRepositoryUrl("systemImageRepositoryUrl")
                                .build())
                .scmRepository(SCMRepository.builder().externalUrl("externalurl").build())
                .scmTag("scmtag")
                .scmRevision("scmrevision")
                .scmUrl("scmurl")
                .buildConfigRevision(BuildConfigurationRevisionRef.refBuilder().id("BCID").build())
                .build();
    }

    @Override
    public BuildConfiguration getBuildConfig(String buildConfigId) {
        return BuildConfiguration.builder()
                .id("BCID")
                .productVersion(ProductVersionRef.refBuilder().id("377").version("1.0").build())
                .build();
    }

    @Override
    public Artifact getArtifact(String purl) {
        return Artifact.builder()
                .id("AA1122")
                .md5("md5")
                .sha1("sha1")
                .sha256("sha256")
                .purl(purl)
                .publicUrl("artifactpublicurl")
                .originUrl("originurl")
                .build(getBuild("dummy"))
                .build();
    }
}