package org.jboss.sbomer.cli.test.utils;

import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.dto.Artifact;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Alternative
@Singleton
@Slf4j
public class KojiServiceAlternative extends KojiService {
    @Override
    public KojiBuild findBuild(Artifact artifact) {
        log.debug("Would look for Build for artifact: {}, returning mocked nothing", artifact.getId());
        return null;
    }
}
