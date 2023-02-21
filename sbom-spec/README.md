# Application Services & Middleware SBOM spec

This is an attempt to define a SBOM specification for Application Services & Middleware products (and their components)
so that interested parties would find the content useful.

## Manifesto

These are the guiding principles for the specification design.

1. We use the [CycloneDX specification](https://cyclonedx.org/specification/overview/) as the base language.
2. We will extend the base language with our specific requirements, if necessary.
3. We put into the SBOM as much information as we can get.
   - Done in phases; low hanging fruits first, but SBOM feature set will grow over time.
4. We will run the SBOM generation for every build in PNC.
   - Retrospectively adding SBOMs for builds done in the past is a possibility.

## Specification

> TBD

## Resources

- https://cyclonedx.org/specification/overview/
- https://github.com/CycloneDX/specification/blob/1.4/schema/bom-1.4.schema.json
- https://cyclonedx.org/docs/1.4/json/
