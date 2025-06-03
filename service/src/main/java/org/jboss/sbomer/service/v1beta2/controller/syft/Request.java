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
package org.jboss.sbomer.service.v1beta2.controller.syft;

import java.util.List;

enum RequestType {
    CONTAINER_IMAGE;
};

record ResourceSpec(String cpu, String memory) {
};

record Resources(ResourceSpec requests, ResourceSpec limits) {
};

record SyftOptions(boolean includeRpms, List<String> paths, String timeout) {
};

record Config(Resources resources, String format, SyftOptions options) {
};

record Generator(String name, String version, Config config) {
};

record Target(RequestType type, String identifier) {
};

public record Request(Generator generator, Target target) {

}
