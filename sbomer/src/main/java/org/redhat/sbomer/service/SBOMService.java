package org.redhat.sbomer.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

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
  SBOMGenerator sbomGenerator;

  /**
   * Runs the generation of SBOM using the available implementation of the
   * generator. This is done in an asynchronous way -- the generation is run
   * behind the scenes.
   * 
   * @param buildId
   */
  public void createBomFromPncBuild(String buildId) {
    sbomGenerator.generate(buildId);
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
