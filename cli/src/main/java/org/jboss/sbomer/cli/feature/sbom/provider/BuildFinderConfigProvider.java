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
package org.jboss.sbomer.cli.feature.sbom.provider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.jboss.pnc.build.finder.core.BuildConfig;
import org.jboss.pnc.build.finder.core.BuildSystem;
import org.jboss.pnc.build.finder.core.ChecksumType;

@ApplicationScoped
public class BuildFinderConfigProvider {

    private static final List<String> DEFAULT_ARCHIVE_EXTENSION = List.of(
            "dll",
            "dylib",
            "ear",
            "jar",
            "jdocbook",
            "jdocbook-style",
            "kar",
            "plugin",
            "pom",
            "rar",
            "sar",
            "so",
            "war",
            "xml");
    private static final List<String> DEFAULT_ARCHIVE_TYPES = Collections
            .unmodifiableList(List.of("jar", "xml", "pom", "so", "dll", "dylib"));
    private static final List<Pattern> DEFAULT_EXCLUDES = Collections
            .unmodifiableList(List.of(Pattern.compile("^(?!.*/pom\\.xml$).*/.*\\.xml$")));
    private static final Set<ChecksumType> DEFAULT_CHECKSUM_TYPES = Collections
            .unmodifiableSet(Set.of(ChecksumType.sha1, ChecksumType.sha256, ChecksumType.md5));
    private static final List<BuildSystem> DEFAULT_BUILD_SYSTEMS = Collections
            .unmodifiableList(List.of(BuildSystem.koji));
    private static final Integer DEFAULT_KOJI_MULTICALL_SIZE = 4;
    private static final Integer DEFAULT_KOJI_NUM_THREADS = 4;

    private BuildConfig config;

    @Produces
    public BuildConfig getConfig() {
        return config;
    }

    public BuildFinderConfigProvider() throws IOException {
        config = new BuildConfig();

        // The archive-extensions option specifies the Koji archive type extensions to include in the archive search. If
        // this option is given, it will override the archive-types option and only files matching the extensions will
        // have their checksums taken.
        config.setArchiveExtensions(DEFAULT_ARCHIVE_EXTENSION);
        // The archive-types option specifies the Koji archive types to include in the archive search.
        config.setArchiveTypes(DEFAULT_ARCHIVE_TYPES);
        // The build system to search for builds
        config.setBuildSystems(DEFAULT_BUILD_SYSTEMS);
        // The checksum-only option specifies whether to skip the Koji build lookup stage and only checksum the files in
        // the input.
        config.setChecksumOnly(false);
        // The checksum-type option specifies the checksum type to use for lookups. Note that at this time Koji can only
        // support a single checksum type in its database, md5, even though the Koji API currently provides additional
        // support for sha256 and sha512 checksum types.
        config.setChecksumTypes(DEFAULT_CHECKSUM_TYPES);
        // The disable-cache option disables the local infinispan cache for checksums and builds.
        config.setDisableCache(true);
        // The disable-recursion option disables recursion when examining archives.
        config.setDisableRecursion(true);
        // The excludes option is list of regular expression patterns. Any paths that match any of these patterns will
        // be excluded during the build-lookup stage search.
        config.setExcludes(DEFAULT_EXCLUDES);
        // The koji-multicall-size option sets the Koji multicall size.
        config.setKojiMulticallSize(DEFAULT_KOJI_MULTICALL_SIZE);
        // The koji-num-threads option sets the number of Koji threads.
        config.setKojiNumThreads(DEFAULT_KOJI_NUM_THREADS);

        // The output-directory option specifies the directory to use for output.
        config.setOutputDirectory(Files.createTempDirectory("sbomer-").toAbsolutePath().toString());

        setKojiHubURL(config);
        setKojiWebURL(config);
    }

    /**
     * Override koji hub url in the config if 'sbomer.koji.hub-url' defined in a system property, env variable, or in
     * application.properties.
     *
     * @param config config file to potentially override its kojiHubUrl
     *
     * @throws IOException if we can't parse the value as an URL
     */
    private void setKojiHubURL(BuildConfig config) throws IOException {
        Optional<String> optionalKojiHubURL = org.eclipse.microprofile.config.ConfigProvider.getConfig()
                .getOptionalValue("sbomer.koji.hub-url", String.class);

        if (optionalKojiHubURL.isPresent()) {
            String s = optionalKojiHubURL.get();

            try {
                URL kojiHubURL = new URL(s);
                config.setKojiHubURL(kojiHubURL);
            } catch (MalformedURLException e) {
                throw new IOException("Bad Koji hub URL: " + s, e);
            }
        }
    }

    /**
     * Override koji web url in the config if 'sbomer.koji.web-url' defined in a system property, env variable, or in
     * application.properties. Otherwise, use kojiHubUrl to generate the kojiWebUrl.
     *
     * @param config config file to potentially override its kojiWebUrl
     *
     * @throws IOException if we can't parse the value as an URL
     */
    private void setKojiWebURL(BuildConfig config) throws IOException {
        Optional<String> optionalKojiWebURL = org.eclipse.microprofile.config.ConfigProvider.getConfig()
                .getOptionalValue("sbomer.koji.web-url", String.class);

        if (optionalKojiWebURL.isPresent()) {
            String s = optionalKojiWebURL.get();

            try {
                URL kojiWebURL = new URL(s);
                config.setKojiWebURL(kojiWebURL);
            } catch (MalformedURLException e) {
                throw new IOException("Bad Koji web URL: " + s, e);
            }
        } else if (config.getKojiWebURL() == null && config.getKojiHubURL() != null) {
            // Hack for missing koji.web-url
            String s = config.getKojiHubURL().toExternalForm().replace("hub.", "web.").replace("hub", "");

            try {
                URL kojiWebURL = new URL(s);
                config.setKojiWebURL(kojiWebURL);
            } catch (MalformedURLException e) {
                throw new IOException("Bad Koji web URL: " + s, e);
            }
        }
    }

}
