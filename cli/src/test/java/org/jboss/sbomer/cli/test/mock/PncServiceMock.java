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
package org.jboss.sbomer.cli.test.mock;

import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.TargetRepository;
import org.jboss.pnc.dto.User;
import org.jboss.sbomer.cli.service.PNCService;

import io.quarkus.test.Mock;
import lombok.extern.slf4j.Slf4j;

@Mock
@Slf4j
public class PncServiceMock extends PNCService {

    @Override
    public Build getBuild(String buildId) {
        return null;
    }

    public Configuration getClientConfiguration() {
        return this.getConfiguration();
    }

    @Override
    public Artifact getArtifact(String purl) {
        log.debug("getArtifact {}", purl);
        if ("pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar".equalsIgnoreCase(purl)) {
            return createArtifactMock1();
        } else if ("pkg:maven/com.beust/jcommander@1.72?type=jar".equalsIgnoreCase(purl)) {
            return createArtifactMock2();
        } else if ("pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar"
                .equalsIgnoreCase(purl)) {
            return createArtifactMock3();
        } else if ("pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar".equalsIgnoreCase(purl)) {
            return createArtifactMock4();
        }

        return null;
    }

    private Artifact createArtifactMock1() {

        Environment environment = Environment.builder()
                .systemImageRepositoryUrl("quay.io/rh-newcastle")
                .systemImageId("builder-rhel-7-j11-mvn3.6.3:1.0.4")
                .build();
        SCMRepository scmRepository = SCMRepository.builder()
                .externalUrl("")
                .internalUrl("git+ssh://code.engineering.redhat.com/vaadin/android-json.git")
                .build();

        Build build = Build.builder()
                .buildContentId("build-MOCKMOCKMOCK1")
                .environment(environment)
                .id("MOCKMOCKMOCK1")
                .scmRepository(scmRepository)
                .scmRevision("6f25bf15308ee95ef5a9783412be2b0557c9046d")
                .scmTag("0.0.20131108.vaadin1")
                .scmUrl("https://code.engineering.redhat.com/gerrit/vaadin/android-json.git")
                .build();

        return Artifact.builder()
                .build(build)
                .deployPath(
                        "/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.jar")
                .deployUrl(
                        "https://indy-gateway.indy.svc.cluster.local/api/content/maven/hosted/pnc-builds/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.jar")
                .filename("android-json-0.0.20131108.vaadin1.jar")
                .id("9748222")
                .identifier("com.vaadin.external.google:android-json:jar:0.0.20131108.vaadin1")
                .md5("b4156881267c9cd11e53c8f2a4ea8cb3")
                .originUrl("")
                .publicUrl(
                        "https://indy.psi.redhat.com/api/content/maven/hosted/pnc-builds/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.jar")
                .purl("pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar")
                .sha1("18484eb6dd563c5a6ef90fed3a2c4ffa1798aba2")
                .sha256("0f659386390f85fbad7d2fcfbb005d38e9971e5369077de7311c07e2656e7d5f")
                .size(9999L)
                .artifactQuality(org.jboss.pnc.enums.ArtifactQuality.NEW)
                .buildCategory(org.jboss.pnc.enums.BuildCategory.STANDARD)
                .creationUser(User.builder().build())
                .modificationUser(User.builder().build())
                .targetRepository(TargetRepository.refBuilder().build())
                .build();
    }

    private Artifact createArtifactMock2() {

        Environment environment = Environment.builder()
                .systemImageRepositoryUrl("quay.io/rh-newcastle")
                .systemImageId("builder-rhel-7-j11-mvn3.6.3:1.0.5")
                .build();
        SCMRepository scmRepository = SCMRepository.builder()
                .externalUrl("https://github.com/beust/jcommander.git")
                .internalUrl("git+ssh://code.engineering.redhat.com/beust/jcommander.git")
                .build();

        Build build = Build.builder()
                .buildContentId("build-MOCKMOCKMOCK2")
                .environment(environment)
                .id("MOCKMOCKMOCK2")
                .scmRepository(scmRepository)
                .scmRevision("6d25bf15308ee95ef5a9783412be2b0557c9046d")
                .scmTag("1.72")
                .scmUrl("https://code.engineering.redhat.com/gerrit/beust/jcommander.git")
                .build();

        return Artifact.builder()
                .build(build)
                .deployPath("/com/beust/jcommander/1.72/jcommander-1.72.jar")
                .deployUrl(
                        "https://indy-gateway.indy.svc.cluster.local/api/content/maven/hosted/pnc-builds/com/beust/jcommander/1.72/jcommander-1.72.jar")
                .filename("jcommander-1.72.jar")
                .id("13748222")
                .identifier("com.beust:jcommander:jar:1.72")
                .md5("c4156881267c9cd11e53c8f2a4ea8cb3")
                .originUrl("https://maven.repository.redhat.com/ga/com/beust/jcommander/1.72/jcommander-1.72.jar")
                .publicUrl(
                        "https://indy.psi.redhat.com/api/content/maven/hosted/pnc-builds/com/beust/jcommander/1.72/jcommander-1.72.jar")
                .purl("pkg:maven/com.beust/jcommander@1.72?type=jar")
                .sha1("13484eb6dd563c5a6ef90fed3a2c4ffa1798aba2")
                .sha256("0d659386390f85fbad7d2fcfbb005d38e9971e5369077de7311c07e2656e7d5f")
                .size(131313L)
                .artifactQuality(org.jboss.pnc.enums.ArtifactQuality.NEW)
                .buildCategory(org.jboss.pnc.enums.BuildCategory.STANDARD)
                .creationUser(User.builder().build())
                .modificationUser(User.builder().build())
                .targetRepository(TargetRepository.refBuilder().build())
                .build();
    }

    private Artifact createArtifactMock3() {

        Environment environment = Environment.builder()
                .systemImageRepositoryUrl("quay.io/rh-newcastle")
                .systemImageId("builder-rhel-7-j11-mvn3.6.8:1.0.0")
                .build();
        SCMRepository scmRepository = SCMRepository.builder()
                .externalUrl("https://github.com/hyperxpro/Brotli4j.git")
                .internalUrl("git+ssh://code.engineering.redhat.com/hyperxpro/Brotli4j.git")
                .build();

        Build build = Build.builder()
                .buildContentId("build-MOCKMOCKMOCK3")
                .environment(environment)
                .id("MOCKMOCKMOCK3")
                .scmRepository(scmRepository)
                .scmRevision("6f25bf15308ee95ef5a9783412be2b0557c9046d")
                .scmTag("1.8.0.redhat-00003")
                .scmUrl("https://code.engineering.redhat.com/gerrit/hyperxpro/Brotli4j.git")
                .build();

        return Artifact.builder()
                .build(build)
                .deployPath("com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .deployUrl(
                        "https://indy-gateway.indy.svc.cluster.local/api/content/maven/hosted/pnc-builds/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .filename("brotli4j-1.8.0.redhat-00003.jar")
                .id("15748222")
                .identifier("com.aayushatharva.brotli4j:brotli4j:jar:1.8.0.redhat-00003")
                .md5("c4156881267c9cd11e53c8f2a4ea8cb3")
                .originUrl(
                        "https://maven.repository.redhat.com/ga/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .publicUrl(
                        "https://indy.psi.redhat.com/api/content/maven/hosted/pnc-builds/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .purl("pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar")
                .sha1("1fbe7cf5e48440584eee5b8da33f009bee730f9e")
                .sha256("75efe10bfb9d1e96c320ab9ca9daddc2aebfcc9d017be651f60cb41ed100f23f")
                .size(131313131399L)
                .artifactQuality(org.jboss.pnc.enums.ArtifactQuality.NEW)
                .buildCategory(org.jboss.pnc.enums.BuildCategory.STANDARD)
                .creationUser(User.builder().build())
                .modificationUser(User.builder().build())
                .targetRepository(TargetRepository.refBuilder().build())
                .build();
    }

    private Artifact createArtifactMock4() {

        Environment environment = Environment.builder()
                .systemImageRepositoryUrl("quay.io/rh-newcastle")
                .systemImageId("builder-rhel-8-j8-mvn3.5.4-netty-tcnative:1.0.2")
                .build();
        SCMRepository scmRepository = SCMRepository.builder()
                .externalUrl("https://github.com/hyperxpro/Brotli4j.git")
                .internalUrl("git+ssh://code.engineering.redhat.com/hyperxpro/Brotli4j.git")
                .build();

        Build build = Build.builder()
                .buildContentId("build-AVOBVY3O23YAA")
                .environment(environment)
                .id("AVOBVY3O23YAA")
                .scmRepository(scmRepository)
                .scmRevision("6f25bf15308ee95ef5a9783412be2b0557c9046d")
                .scmTag("1.8.0.redhat-00003")
                .scmUrl("https://code.engineering.redhat.com/gerrit/hyperxpro/Brotli4j.git")
                .build();

        return Artifact.builder()
                .build(build)
                .deployPath("com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .deployUrl(
                        "https://indy-gateway.indy.svc.cluster.local/api/content/maven/hosted/pnc-builds/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .filename("brotli4j-1.8.0.redhat-00003.jar")
                .id("15748222")
                .identifier("com.aayushatharva.brotli4j:brotli4j:jar:1.8.0.redhat-00003")
                .md5("c4156881267c9cd11e53c8f2a4ea8cb3")
                .originUrl(
                        "https://maven.repository.redhat.com/ga/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .publicUrl(
                        "https://indy.psi.redhat.com/api/content/maven/hosted/pnc-builds/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar")
                .purl("pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar")
                .sha1("1fbe7cf5e48440584eee5b8da33f009bee730f9e")
                .sha256("75efe10bfb9d1e96c320ab9ca9daddc2aebfcc9d017be651f60cb41ed100f23f")
                .size(131313131399L)
                .artifactQuality(org.jboss.pnc.enums.ArtifactQuality.NEW)
                .buildCategory(org.jboss.pnc.enums.BuildCategory.STANDARD)
                .creationUser(User.builder().build())
                .modificationUser(User.builder().build())
                .targetRepository(TargetRepository.refBuilder().build())
                .build();
    }

}
