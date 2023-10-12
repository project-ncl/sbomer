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
package org.jboss.sbomer.core.features.sbom;

public class Constants {

    public static final String SBOM_RED_HAT_PNC_BUILD_ID = "pnc-build-id";
    public static final String SBOM_RED_HAT_BREW_BUILD_ID = "brew-build-id";
    public static final String SBOM_RED_HAT_PUBLIC_URL = "public-url";
    public static final String SBOM_RED_HAT_ORIGIN_URL = "origin-url";
    public static final String SBOM_RED_HAT_BUILD_SYSTEM = "build-system";
    public static final String SBOM_RED_HAT_SCM_URL = "scm-url";
    public static final String SBOM_RED_HAT_SCM_REVISION = "scm-revision";
    public static final String SBOM_RED_HAT_SCM_TAG = "scm-tag";
    public static final String SBOM_RED_HAT_SCM_EXTERNAL_URL = "scm-external-url";
    public static final String SBOM_RED_HAT_ENVIRONMENT_IMAGE = "pnc-environment-image";

    public static final String PNC_BUILD_SYSTEM = "PNC";
    public static final String BREW_BUILD_SYSTEM = "BREW";

    public static final String DISTRIBUTION = "Red Hat distribution";
    public static final String PUBLISHER = "Red Hat";
    public static final String SUPPLIER_NAME = "Red Hat";
    public static final String SUPPLIER_URL = "https://www.redhat.com";
    public static final String MRRC_URL = "https://maven.repository.redhat.com/ga/";

    public final static String PROPERTY_ERRATA_PRODUCT_NAME = "errata-tool-product-name";
    public final static String PROPERTY_ERRATA_PRODUCT_VERSION = "errata-tool-product-version";
    public final static String PROPERTY_ERRATA_PRODUCT_VARIANT = "errata-tool-product-variant";

    public final static String BUILD_ATTRIBUTES_BREW_BUILD_VERSION = "BREW_BUILD_VERSION";
    public final static String GRADLE_MAJOR_VERSION_COMMAND_PREFIX = "GRADLE_MAJOR_VERSION=";
    public final static String GRADLE_PLUGIN_VERSION_ENV_VARIABLE = "PLUGIN_VERSION";

    /**
     * The label name that identifies the particular Sbom resource.
     */
    public static String TEKTON_LABEL_SBOM_ID = "sbomer.jboss.org/sbom-id";

    /**
     * The label name that identifies the particular Sbom build id.
     */
    public static String TEKTON_LABEL_SBOM_BUILD_ID = "sbomer.jboss.org/sbom-build-id";

    /**
     * Default Kubernetes label: app.kubernetes.io/part-of
     */
    public static String TEKTON_LABEL_NAME_APP_PART_OF = "app.kubernetes.io/part-of";

    /**
     * Value for the default Kubernetes label: app.kubernetes.io/part-of: sbomer
     */
    public static String TEKTON_LABEL_VALUE_APP_PART_OF = "sbomer";

    /**
     * The suffix which is used in a Task Run name to identify the number of retry attempt
     */
    public static String TEKTON_TASK_RUN_NAME_SUFFIX_RETRY_ATTEMPT = "retry";

}
