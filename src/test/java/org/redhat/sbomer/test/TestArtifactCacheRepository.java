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
package org.redhat.sbomer.test;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.jboss.pnc.common.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.redhat.sbomer.dto.ArtifactInfo;
import org.redhat.sbomer.model.ArtifactCache;
import org.redhat.sbomer.repositories.ArtifactCacheRepository;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.arc.Priority;
import io.quarkus.logging.Log;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Priority(1)
@Alternative
@ApplicationScoped
@QuarkusTransactionalTest
public class TestArtifactCacheRepository extends ArtifactCacheRepository {

    private ArtifactCache createArtifactCache() throws IOException {
        String info = TestResources.asString("sboms/artifact-info.json");
        JsonNode artifactInfoNode = JsonUtils.fromJson(info, JsonNode.class);
        ArtifactCache artifactCache = new ArtifactCache();
        artifactCache.setPurl("pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar");
        artifactCache.setId(516640206274228224L);
        artifactCache.setInfo(artifactInfoNode);
        return artifactCache;
    }

    @PostConstruct
    public void init() {
        try {
            ArtifactCache artifactCache = createArtifactCache();
            saveArtifactCache(artifactCache);
        } catch (IOException exc) {
            Log.error("Failed to persist new artifact cache", exc);
        }
    }

    @Test
    public void testGetArtifactCache() throws IOException {
        ArtifactCache artifactCache = getArtifactCache(
                "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar");
        assertEquals(516640206274228224L, artifactCache.getId());
        assertEquals(
                "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar",
                artifactCache.getPurl());

        JsonNode artifactInfoNode = JsonUtils.fromJson(artifactCache.getInfo().asText(), JsonNode.class);
        String md5 = artifactInfoNode.path("md5").asText();
        String sha256 = artifactInfoNode.path("sha256").asText();
        String buildId = artifactInfoNode.path("buildId").asText();
        assertEquals("493ac341ddf64842497573deca9bee8c", md5);
        assertEquals("75efe10bfb9d1e96c320ab9ca9daddc2aebfcc9d017be651f60cb41ed100f23f", sha256);
        assertEquals("AVOBVY3O23YAA", buildId);
    }

    @Test
    public void testGetArtifactCacheWithParser() throws IOException {
        ArtifactCache artifactCache = getArtifactCache(
                "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar");
        assertEquals(516640206274228224L, artifactCache.getId());
        assertEquals(
                "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar",
                artifactCache.getPurl());

        ArtifactInfo artifactInfo = JsonUtils.fromJson(artifactCache.getInfo().asText(), ArtifactInfo.class);
        assertEquals("493ac341ddf64842497573deca9bee8c", artifactInfo.getMd5());
        assertEquals("75efe10bfb9d1e96c320ab9ca9daddc2aebfcc9d017be651f60cb41ed100f23f", artifactInfo.getSha256());
        assertEquals("AVOBVY3O23YAA", artifactInfo.getBuildId());

        String jsonString = JsonUtils.toJson(artifactInfo);
        JsonNode node = JsonUtils.fromJson(jsonString, JsonNode.class);
        String md5 = node.path("md5").asText();
        String sha256 = node.path("sha256").asText();
        String buildId = node.path("buildId").asText();
        assertEquals("493ac341ddf64842497573deca9bee8c", md5);
        assertEquals("75efe10bfb9d1e96c320ab9ca9daddc2aebfcc9d017be651f60cb41ed100f23f", sha256);
        assertEquals("AVOBVY3O23YAA", buildId);
    }

}