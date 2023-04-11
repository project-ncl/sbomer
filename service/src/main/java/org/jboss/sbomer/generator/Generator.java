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
package org.jboss.sbomer.generator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import org.jboss.sbomer.core.enums.GeneratorImplementation;

@Qualifier
@Retention(RUNTIME)
@Target({ FIELD, TYPE })
public @interface Generator {
    public GeneratorImplementation value();

    @SuppressWarnings("all")
    public final class GeneratorLiteral extends AnnotationLiteral<Generator> implements Generator {
        private static final long serialVersionUID = 1L;

        private final GeneratorImplementation value;

        public static GeneratorLiteral of(GeneratorImplementation value) {
            return new GeneratorLiteral(value);
        }

        private GeneratorLiteral(GeneratorImplementation value) {
            this.value = value;
        }

        @Override
        public GeneratorImplementation value() {
            return value;
        }
    }
}
