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
 * Error indicating that the PNC build type is not yet supported in SBOMer.
 */
public class UnsupportedPncBuildException extends CommandLineException {
    private static final int EXIT_CODE = 32;

    @Override
    public int getExitCode() {
        return UnsupportedPncBuildException.EXIT_CODE;
    }

    public UnsupportedPncBuildException(String msg, Object... params) {
        super(msg, params);
    }

    public UnsupportedPncBuildException(ApplicationException e) {
        super(e);
    }

}
