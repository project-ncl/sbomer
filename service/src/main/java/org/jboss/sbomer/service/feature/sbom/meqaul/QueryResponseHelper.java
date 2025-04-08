/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.feature.sbom.meqaul;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.sbomer.service.feature.sbom.meqaul.dto.MequalQueryResponse;
import org.jboss.sbomer.service.feature.sbom.meqaul.dto.QueryPayload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class QueryResponseHelper {

    public static Path toPath(MequalQueryResponse resp, Path bomPath) throws IOException {
        BufferedWriter bw = null;
        Path mequalFile = Path.of(bomPath.getParent().toString(), "mequal.json");
        try {
            File mfh = mequalFile.toFile();
            if (!mfh.exists()) {
                mfh.createNewFile();
                FileWriter fw = new FileWriter(mfh);
                bw = new BufferedWriter(fw);
                bw.write(resp.toString());
            }
        } catch (IOException e) {
            throw new IOException("Error while processing file: " + mequalFile, e);
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                throw new IOException("Error while closing file: " + mequalFile.toString());
            }
        }
        return mequalFile;
    }
}
