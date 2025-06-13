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
package org.jboss.sbomer.service.nextgen.core.dto.api;

/**
 * Identifier of the deliverable to be manifested.
 *
 * @param type Type of the deliverable. This should be set to a type that is supported by particular SBOMer deployment,
 *        for example: {@code CONTAINER_IMAGE}.
 * @param identifier An identifier of the deliverable that is meaningful in the context of the {@code type} param, for
 *        example:
 *        {@code registry.access.redhat.com/ubi8@sha256:0c1757c4526cfd7fdfedc54fadf4940e7f453201de65c0fefd454f3dde117273}.
 */
public record Target(String type, String identifier) {
}