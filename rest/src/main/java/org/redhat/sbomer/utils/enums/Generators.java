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
package org.redhat.sbomer.utils.enums;

import javax.enterprise.util.AnnotationLiteral;

import org.redhat.sbomer.generator.CycloneDX;
import org.redhat.sbomer.generator.Domino;

public enum Generators {

    DOMINO(Domino.Literal.INSTANCE), CYCLONEDX(CycloneDX.Literal.INSTANCE);

    private AnnotationLiteral selector;

    private Generators(AnnotationLiteral selector) {
        this.selector = selector;
    }

    public AnnotationLiteral getSelector() {
        return this.selector;
    }

}
