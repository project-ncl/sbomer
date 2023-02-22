package org.redhat.sbomer.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.pnc.dto.Build;
import org.redhat.sbomer.model.SBOM;
import org.redhat.sbomer.service.generator.SBOMGenerator;

/**
 * Main SBOM service that is dealing with the {@link SBOM} resource.
 */
@ApplicationScoped
public class SBOMService {
  @Inject
  EntityManager em;

  @Inject
  PNCService pncService;

  @Inject
  SBOMGenerator sbomGenerator;

  public SBOM createBomFromPncBuild(String buildId) {
    Build build = pncService.getBuild(buildId);

    System.out.println(build.getScmUrl());
    System.out.println(build.getScmRevision());
    System.out.println(build.getEnvironment().getSystemImageId());

    return null;
  }

  /**
   * List all {@link SBOM} instances we know about.
   * 
   * TODO: This shouldn't be really exposed this way. It's for now to speed up
   * development.
   * 
   * @return
   */
  public List<SBOM> listBoms() {
    return em.createQuery("from SBOM", SBOM.class).getResultList();
  }

  public SBOM getBom(String id) {
    return em.find(SBOM.class, id);
  }

  /**
   * Persist changes to the {@link SBOM} in the database.
   * 
   * @param sbom
   * @return
   */
  @Transactional
  public SBOM saveBom(SBOM sbom) {
    em.merge(sbom);
    return sbom;
  }
}
