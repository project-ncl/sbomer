package org.redhat.sbomer.service;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;

@ApplicationScoped
public class PNCService {

  private Configuration getConfiguration() {
    return Configuration.builder().host("orch.psi.redhat.com").protocol("http").build();
  }

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
