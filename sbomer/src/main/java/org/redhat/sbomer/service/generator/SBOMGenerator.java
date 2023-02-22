package org.redhat.sbomer.service.generator;

/**
 * High-level interaction with the SBOM generator.
 * 
 */
public interface SBOMGenerator {
  /**
   * Generates the SBOM in CycloneDX format for a project located in git under the
   * provided coordinates.
   * 
   * @param url
   * @param revision
   */
  public void generate(String url, String revision);
}
