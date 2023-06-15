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
package org.jboss.sbomer.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.enums.ProcessorType;

import lombok.Builder;
import lombok.Data;

/**
 * A runtime execution configuration for a particular processing execution.
 *
 * @author Marek Goldmann
 */
@Data
@Builder
public class ProcessingExecConfig {
    @Data
    @Builder
    public static class ProcessorExec {
        ProcessorType processor;
        String version;
        @Builder.Default
        String args = "";

        public String processorSlug() {
            return processor.name().toLowerCase().replaceAll("_", "-");
        }
    }

    @Builder.Default
    List<ProcessorExec> processors = new ArrayList<>();

    public String processorsCommand() {
        return getProcessors().stream()
                .map(p -> new String(p.processorSlug() + " " + p.getArgs()).strip())
                .collect(Collectors.joining(" "));
    }
}
