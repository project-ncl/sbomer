package org.redhat.sbomer.service.generator;

import org.redhat.sbomer.errors.ApplicationException;

/**
 * High-level interaction with the SBOM generator.
 * 
 */
public interface SBOMGenerator {
  /**
   * Generates the SBOM in CycloneDX format for a PNC build identified by the
   * buildId
   * 
   * @param buildId PNC build id
   */
  public void generate(String buildId) throws ApplicationException;
}
