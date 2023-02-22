package org.redhat.sbomer.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.redhat.sbomer.model.SBOM;

/**
 * Main SBOM service that is dealing with the {@link SBOM} resource.
 */
@ApplicationScoped
public class SBOMService {
  @Inject
  EntityManager em;

  /**
   * List all {@link SBOM} instances we know about.
   * 
   * TODO: This shouldn't be really exposed this way. It's for now to speed up
   * development.
   * 
   * @return
   */
  public List<SBOM> list() {
    return em.createQuery("from SBOM", SBOM.class).getResultList();
  }

  /**
   * Persist changes to the {@link SBOM} in the database.
   * 
   * @param sbom
   * @return
   */
  @Transactional
  public SBOM save(SBOM sbom) {
    em.merge(sbom);
    return sbom;
  }
}
