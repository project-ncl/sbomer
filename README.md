# SBOMer

A service to generate SBOMs in the CycloneDX format for different source types.

## Modifications
- [2024-12-06]: Modified `org.cyclonedx.model.Dependency.java` and `org.cyclonedx.util.serializer.DependencySerializer` to temporarily workaround the missing mapping of 'provides', which was introduced by the CycloneDX v1.6 specification.
