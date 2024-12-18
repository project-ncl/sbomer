# SBOMer

A service to generate SBOMs in the CycloneDX format for different source types.

## Modifications
- [2024-12-06]: Modified `org.cyclonedx.model.Bom`, `org.cyclonedx.model.BomReference`, `org.cyclonedx.model.Dependency.java`, `org.cyclonedx.model.DependencyList`, `org.cyclonedx.parsers.JsonParser`, `org.cyclonedx.parsers.Parser`, `org.cyclonedx.util.deserializer.DependencyDeserializer`,  `org.cyclonedx.util.serializer.DependencySerializer`, `org.cyclonedx.generators.AbstractBomGenerator`, `org.cyclonedx.generators.BomGeneratorFactory`, `org.cyclonedx.generators.json.DependencySerializer`.`org.cyclonedx.generators.xml.BomXmlGenerator` to temporarily workaround the missing mapping of 'provides', which was introduced by the CycloneDX v1.6 specification.

