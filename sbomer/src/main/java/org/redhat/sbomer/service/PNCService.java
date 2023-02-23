package org.redhat.sbomer.service;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;

/**
 * A service to interact with the PNC build system.
 */
@ApplicationScoped
public class PNCService {

    /**
     * Setup basic configuration to be able to talk to PNC.
     *
     * TODO: Make it configurable -- move to ConfigMap.
     *
     * @return
     */
    private Configuration getConfiguration() {
        return Configuration.builder().host("orch.psi.redhat.com").protocol("http").build();
    }

    /**
     * Fetch information about the PNC {@link Build} identified by the particular buildId.
     *
     * @param buildId
     * @return
     */
    public Build getBuild(String buildId) {
        BuildClient client = new BuildClient(getConfiguration());

        try {
            return client.getSpecific(buildId);
        } catch (RemoteResourceException e) {
            e.printStackTrace();
        }

        // TODO: Change this to throw an exception instead
        return null;

    }

}
