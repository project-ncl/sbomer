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
package org.jboss.sbomer.service.nextgen.controller.tekton;

import java.util.Set;

import org.jboss.sbomer.service.nextgen.controller.Controller;

import io.fabric8.tekton.v1beta1.TaskRun;

/**
 * Controller following the operator pattern for a given resource. It is a special case of controller that brings
 * together the main resource as well as secondary resources which are Tekton {@link TaskRun}.
 */
public interface TektonController<R> extends Controller {

    /**
     * Main method to perform reconciliation on the provided resource taking into account secondary {@link TaskRun}
     * resources related to the main resource.
     *
     * @param resource
     * @param relatedTaskRuns
     */
    void reconcile(R resource, Set<TaskRun> relatedTaskRuns);

    /**
     * Method to generate a TaskRun that should be created for a given main resource.
     */
    TaskRun desired(R resource);
}
