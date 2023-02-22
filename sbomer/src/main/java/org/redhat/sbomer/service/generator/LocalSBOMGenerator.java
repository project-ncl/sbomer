package org.redhat.sbomer.service.generator;

import java.io.File;
import java.nio.file.Files;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.jgit.api.Git;

@ApplicationScoped
public class LocalSBOMGenerator implements SBOMGenerator {

  @Override
  public void generate(String url, String revision) {
    try {
      fetchSourceCode(url, revision);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // WIP
  private void fetchSourceCode(String url, String revision) throws Exception {
    File tmpDir = Files.createTempDirectory("sbomer").toFile();

    try (Git git = Git.cloneRepository().setDirectory(tmpDir).setURI(url).call()) {
      git.checkout().setName(revision).call();
    }
  }
}
