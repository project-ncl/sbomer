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
package org.jboss.sbomer.cli.errors.pnc;

import org.jboss.sbomer.cli.errors.CommandLineException;
import org.jboss.sbomer.core.errors.ApplicationException;

/**
 * Error indicating a failure related to the build progress or status received from PNC.
 */
public class InvalidPncBuildStateException extends CommandLineException {
    private static final int EXIT_CODE = 31;

    public InvalidPncBuildStateException(String msg, Object... params) {
        super(InvalidPncBuildStateException.EXIT_CODE, msg, params);
    }

    public InvalidPncBuildStateException(ApplicationException e) {
        super(InvalidPncBuildStateException.EXIT_CODE, e);
    }

}
