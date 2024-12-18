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
package org.jboss.sbomer.core.patch.cyclonedx.generators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.cyclonedx.CycloneDxSchema;
import org.cyclonedx.Format;
import org.cyclonedx.Version;
import org.cyclonedx.util.serializer.CustomSerializerModifier;
import org.cyclonedx.util.serializer.EvidenceSerializer;
import org.cyclonedx.util.serializer.ExternalReferenceSerializer;
import org.cyclonedx.util.serializer.HashSerializer;
import org.cyclonedx.util.serializer.InputTypeSerializer;
import org.cyclonedx.util.serializer.LicenseChoiceSerializer;
import org.cyclonedx.util.serializer.LifecycleSerializer;
import org.cyclonedx.util.serializer.MetadataSerializer;
import org.cyclonedx.util.serializer.OutputTypeSerializer;
import org.cyclonedx.util.serializer.SignatorySerializer;
import org.jboss.sbomer.core.patch.cyclonedx.model.Bom;

/*
 * Modified by Andrea Vibelli to temporarily fix https://github.com/CycloneDX/cyclonedx-core-java/issues/565.
 */
public abstract class AbstractBomGenerator extends CycloneDxSchema {
    protected ObjectMapper mapper;

    protected final Version version;

    protected Bom bom;

    protected final Format format;

    public AbstractBomGenerator(final Version version, final Bom bom, final Format format) {
        this.mapper = new ObjectMapper();
        this.version = version;
        this.bom = bom;
        this.format = format;

        if (!version.getFormats().contains(format)) {
            throw new IllegalArgumentException(
                    "CycloneDX version " + version.getVersionString() + " does not support the " + format + " format");
        }
    }

    /**
     * Returns the version of the CycloneDX schema used by this instance
     *
     * @return a CycloneDxSchemaVersion enum
     */
    public Version getSchemaVersion() {
        return version;
    }

    /**
     * Returns the format that this generator creates.
     *
     * @return a Format enum
     */
    public Format getFormat() {
        return format;
    }

    protected void setupObjectMapper(boolean isXml) {
        SimpleModule licenseModule = new SimpleModule();
        licenseModule.addSerializer(new LicenseChoiceSerializer(isXml, version));
        mapper.registerModule(licenseModule);

        SimpleModule lifecycleModule = new SimpleModule();
        lifecycleModule.addSerializer(new LifecycleSerializer(isXml));
        mapper.registerModule(lifecycleModule);

        SimpleModule metadataModule = new SimpleModule();
        metadataModule.addSerializer(new MetadataSerializer(isXml, getSchemaVersion()));
        mapper.registerModule(metadataModule);

        SimpleModule inputTypeModule = new SimpleModule();
        inputTypeModule.addSerializer(new InputTypeSerializer(isXml));
        mapper.registerModule(inputTypeModule);

        SimpleModule outputTypeModule = new SimpleModule();
        outputTypeModule.addSerializer(new OutputTypeSerializer(isXml));
        mapper.registerModule(outputTypeModule);

        SimpleModule evidenceModule = new SimpleModule();
        evidenceModule.addSerializer(new EvidenceSerializer(isXml, getSchemaVersion()));
        mapper.registerModule(evidenceModule);

        SimpleModule signatoryModule = new SimpleModule();
        signatoryModule.addSerializer(new SignatorySerializer(isXml));
        mapper.registerModule(signatoryModule);

        SimpleModule externalSerializer = new SimpleModule();
        externalSerializer.addSerializer(new ExternalReferenceSerializer(getSchemaVersion()));
        mapper.registerModule(externalSerializer);

        SimpleModule hash1Module = new SimpleModule();
        hash1Module.addSerializer(new HashSerializer(version));
        mapper.registerModule(hash1Module);

        SimpleModule propertiesModule = new SimpleModule();
        propertiesModule.setSerializerModifier(new CustomSerializerModifier(isXml, version));
        mapper.registerModule(propertiesModule);
    }
}
