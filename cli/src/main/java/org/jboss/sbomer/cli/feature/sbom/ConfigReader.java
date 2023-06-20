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
package org.jboss.sbomer.cli.feature.sbom;

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.cli.feature.sbom.client.GitilesClient;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.features.sbomer.config.runtime.Config;
import org.jboss.sbomer.core.features.sbomer.utils.ObjectMapperProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConfigReader {
    private static String CONFIG_PATH = ".sbomer/config.yaml";

    @Inject
    @RestClient
    GitilesClient gitilesClient;

    @Getter
    ObjectMapper yamlObjectMapper = ObjectMapperProvider.yaml();

    @Getter
    ObjectMapper jsonObjectMapper = ObjectMapperProvider.json();

    public Config getConfig(Build build) {
        Pattern pattern = Pattern.compile("gerrit/(.*)\\.git$");
        Matcher matcher = pattern.matcher(build.getScmUrl());

        if (!matcher.find()) {
            throw new ClientException(
                    "Unable to determine the project from the SCM url: '{}' from PNC build '{}'",
                    build.getScmUrl(),
                    build.getId());
        }

        String repository = matcher.group(1);

        log.debug("Found Gerrit project: '{}'", repository);

        String base64Config;

        log.debug(
                "Fetching file '{}' from the '{}' repository with tag '{}'",
                CONFIG_PATH,
                repository,
                build.getScmTag());

        try {
            base64Config = gitilesClient.fetchFile(repository, "refs/tags/" + build.getScmTag(), CONFIG_PATH);
        } catch (NotFoundException nfe) {
            log.debug(
                    "SBOMer configuration file not found in the '{}' repository with '{}' tag, ignoring",
                    repository,
                    build.getScmTag());
            return null;
        }

        try {
            return yamlObjectMapper.readValue(Base64.getDecoder().decode(base64Config), Config.class);
        } catch (IOException e) {
            throw new ApplicationException("Could not read configuration file", e);
        }
    }
}
