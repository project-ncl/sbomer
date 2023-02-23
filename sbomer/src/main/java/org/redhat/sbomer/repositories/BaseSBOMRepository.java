package org.redhat.sbomer.repositories;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.redhat.sbomer.model.BaseSBOM;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class BaseSBOMRepository implements PanacheRepositoryBase<BaseSBOM, Long> {

    public BaseSBOM getBaseSbom(String buildId) {
        return find(BaseSBOM.FIND_BY_BUILDID, buildId).singleResult();
    }

    @Transactional
    public BaseSBOM saveBom(BaseSBOM baseSbom) {
        persist(baseSbom);
        return baseSbom;
    }

}