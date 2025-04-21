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
package org.jboss.sbomer.service.rest.faulttolerance;

public class Constants {

    public static final int PNC_CLIENT_MAX_RETRIES = 10;
    public static final int ERRATA_CLIENT_MAX_RETRIES = 15;
    public static final int PYXIS_CLIENT_MAX_RETRIES = 15;
    public static final int ATLAS_CLIENT_MAX_RETRIES = 15;

    public static final long PNC_CLIENT_DELAY = 1;
    public static final long ERRATA_CLIENT_DELAY = 1;
    public static final long PYXIS_CLIENT_DELAY = 1;
    public static final long ATLAS_CLIENT_DELAY = 1;

    /*
     * In some circumstances Pyxis returns a 200 code but an empty RepositoryDescription, this means it may not yet be
     * published, this can take a long time (hours and days). The below settings apply to this scenario, as we are using
     * the fibonaci backoff we can work out the backoff intervals from intial delay and max retries, initially we will
     * aim for 2 days (48hrs) max backoff.
     *
     * Intial Delay = 60 minutes Max retries = 18 (2584 minutes - 1 day 19hrs 4minutes)
     *
     */
    // public static final ChronoUnit PYXIS_UNPUBLISHED_UNIT = ChronoUnit.MINUTES;
    public static final int PYXIS_UNPUBLISHED_INITIAL_DELAY = 8;
    public static final int PYXIS_UNPUBLISHED_MAX_RETRIES = 18;
}
