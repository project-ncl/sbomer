package org.redhat.sbomer.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.redhat.sbomer.model.SBOM;

@ApplicationScoped
public class SBOMService {
  @Inject
  EntityManager em;

  public List<SBOM> list() {
    return em.createQuery("from SBOM", SBOM.class).getResultList();
  }

  @Transactional
  public SBOM create(SBOM sbom) {
    em.merge(sbom);
    return sbom;
  }
}
