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

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.Property;

@Slf4j
public class PurlRebuilder {

    // List taken from https://github.com/anchore/syft/blob/main/syft/pkg/type.go
    private final static String SYFT_ALPMPKG = "alpm";
    private final static String SYFT_APKPKG = "apk";
    private final static String SYFT_BINARYPKG = "binary";
    private final static String SYFT_COCOAPODSPKG = "pod";
    private final static String SYFT_CONANPKG = "conan";
    private final static String SYFT_DARTPUBPKG = "dart-pub";
    private final static String SYFT_DEBPKG = "deb";
    private final static String SYFT_DOTNETPKG = "dotnet";
    private final static String SYFT_ERLANGOTPPKG = "erlang-otp";
    private final static String SYFT_GEMPKG = "gem";
    private final static String SYFT_GITHUBACTIONPKG = "github-action";
    private final static String SYFT_GITHUBACTIONWORKFLOWPKG = "github-action-workflow";
    private final static String SYFT_GOMODULEPKG = "go-module";
    private final static String SYFT_GRAALVMNATIVEIMAGEPKG = "graalvm-native-image";
    private final static String SYFT_HACKAGEPKG = "hackage";
    private final static String SYFT_HEXPKG = "hex";
    private final static String SYFT_JAVAPKG = "java-archive";
    private final static String SYFT_JENKINSPACKAGE = "jenkins-plugin";
    private final static String SYFT_KBPKG = "msrc-kb";
    private final static String SYFT_LINUXKERNELPKG = "linux-kernel";
    private final static String SYFT_LINUXKERNELMODULEPKG = "linux-kernel-module";
    private final static String SYFT_NIXPKG = "nix";
    private final static String SYFT_NPMPKG = "npm";
    private final static String SYFT_PHPCOMPOSERPKG = "php-composer";
    private final static String SYFT_PHPPECLPKG = "php-pecl";
    private final static String SYFT_PORTAGEPKG = "portage";
    private final static String SYFT_PYTHONPKG = "python";
    private final static String SYFT_RPKG = "R-package";
    private final static String SYFT_LUAROCKSPKG = "lua-rocks";
    private final static String SYFT_RPMPKG = "rpm";
    private final static String SYFT_RUSTPKG = "rust-crate";
    private final static String SYFT_SWIFTPKG = "swift";
    private final static String SYFT_SWIPLPACKPKG = "swiplpack";
    private final static String SYFT_OPAMPKG = "opam";
    private final static String SYFT_WORDPRESSPLUGINPKG = "wordpress-plugin";

    // Associations taken from https://github.com/anchore/syft/blob/main/syft/pkg/type.go#L91
    private static final Map<String, String> SYFT_PACKAGE_2_PURL_TYPE_MAP = new HashMap<>();
    static {
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_ALPMPKG, "alpm");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_APKPKG, "apk");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_COCOAPODSPKG, "cocoapods");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_CONANPKG, "conan");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_DARTPUBPKG, "pub");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_DEBPKG, PackageURL.StandardTypes.DEBIAN);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_DOTNETPKG, "dotnet");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_ERLANGOTPPKG, "otp");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_GEMPKG, PackageURL.StandardTypes.GEM);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_GITHUBACTIONPKG, PackageURL.StandardTypes.GITHUB);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_GITHUBACTIONWORKFLOWPKG, PackageURL.StandardTypes.GITHUB);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_GOMODULEPKG, PackageURL.StandardTypes.GOLANG);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_HACKAGEPKG, "hackage");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_HEXPKG, PackageURL.StandardTypes.HEX);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_JAVAPKG, PackageURL.StandardTypes.MAVEN);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_JENKINSPACKAGE, PackageURL.StandardTypes.MAVEN);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_LINUXKERNELPKG, "generic/linux-kernel");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_LINUXKERNELMODULEPKG, PackageURL.StandardTypes.GENERIC);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_NIXPKG, "nix");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_NPMPKG, PackageURL.StandardTypes.NPM);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_PHPCOMPOSERPKG, PackageURL.StandardTypes.COMPOSER);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_PHPPECLPKG, "pecl");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_PORTAGEPKG, "portage");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_PYTHONPKG, PackageURL.StandardTypes.PYPI);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_RPKG, "cran");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_LUAROCKSPKG, "luarocks");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_RPMPKG, PackageURL.StandardTypes.RPM);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_RUSTPKG, PackageURL.StandardTypes.CARGO);
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_SWIFTPKG, "swift");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_SWIPLPACKPKG, "swiplpack");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_OPAMPKG, "opam");
        SYFT_PACKAGE_2_PURL_TYPE_MAP.put(SYFT_WORDPRESSPLUGINPKG, "wordpress-plugin");
    }

    /**
     * Given a component, tries to create a valid purl using the Syft information (if available) and the component
     * properties
     *
     * @param component
     * @return a valid rebuilt purl
     */
    public static String rebuildPurlFromSyftComponent(Component component) throws MalformedPackageURLException {

        Optional<Property> syftPackageType = SbomUtils.findPropertyWithNameInComponent("syft:package:type", component);
        if (!syftPackageType.isPresent()) {
            return null;
        }

        String type = SYFT_PACKAGE_2_PURL_TYPE_MAP.get(syftPackageType.get().getValue());
        if (type == null) {
            return null;
        }

        // Use all the data we have without overthinking about the type of PURL
        String namespace = PurlSanitizer.sanitizeNamespace(component.getGroup());
        String name = PurlSanitizer.sanitizeName(component.getName());
        String version = PurlSanitizer.sanitizeVersion(component.getVersion());

        PackageURL purl = new PackageURL(type, namespace, name, version, null, null);
        return purl.canonicalize();
    }

}
