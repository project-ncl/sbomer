package org.jboss.sbomer.service.test.utils;

import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;

@WithTestResource(value = KubernetesServerTestResource.class, scope = TestResourceScope.GLOBAL)
public class GlobalKubernetesServerResource {

}
