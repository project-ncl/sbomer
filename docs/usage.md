# SBOMer usage

## How it works?

Once a build in PNC finishes SBOMer receives notification about this fact and the generation
process is started. This prepares a "base" SBOM which is then enhanced with the information
available in the builds system as well as other databases.

When the final (processed) SBOM is prepared, it is stored in the SBOMer database and a notification
is sent on the UMB so that other systems will be aware of it.

SBOMs can be fetched from the SBOMer using the REST API.

## Configuration

Although SBOMer was designed in a way it does not require user intervention to generate the SBOM,
there is a requirement to ensure the relationship between the generated SBOM and a particular product
release is maintained.

After suggestion from PST a decision has been made to use the Errata Tool identifiers of the product to
identify the product release for a particular SBOM. This data needs to be provided to SBOMer in a
configuration file.

SBOMer configuration file, besides providing the product mapping, can adjust the generation process as well.
In most cases defaults will be a good fit, but sometimes changes will be required.

Below you can see the structure of this file.

```yaml
# Optional, can be omitted. If not provided -- latest version will be assumed.
apiVersion: sbomer.jboss.org/v1alpha1
# Array of products
products:
  - processors:
      # Custom processors configuration (optional)
      # Please note that this is a map!

      # Specific processor configuration
      # All processors and available configs are listed below
      [PROCESSOR_SLUG]: [PROCESSOR_CONFIG]

    generator:
      # Custom generator configuration (optional)

      # The type of the generator.
      type: [DOMINO|CYCLONEDX]

      # Additional arguments passed to the generator.
      customArgs: [ADDITIONAL_GENERATOR_ARGS]

      # Version of the generator.
      version: [CUSTOM_GENERATOR_VERSION]
```

Please note that the `products` element is an array! This makes it possible to cover the unusual
case where a single build results in multiple product releases.

This file should be stored in the Gerrit repository that is used by the Build Configuration under
the `.sbomer/config.yaml` path.

### Examples

```yaml
# Multi-product example where a single build (configuration) in PNC can build more than
# one product.
apiVersion: sbomer.jboss.org/v1alpha1
products:
  - processors:
      redhat-product:
        errata:
          productName: "CCCDDD"
          productVersion: "CCDD"
          productVariant: "CD"
    generator:
      type: DOMINO
      customArgs: "--config-file .domino/cccddd.json"
      version: "0.0.88"

  - processors:
      redhat-product:
        errata:
          productName: "AAABBB"
          productVersion: "AABB"
          productVariant: "AB"
    generator:
      type: DOMINO
      customArgs: "--config-file .domino/aaabbb.json"
```

```yaml
# Single product with errata configuration override.
# Default configuration for the generator will be used.
apiVersion: sbomer.jboss.org/v1alpha1
products:
  - processors:
      redhat-product:
        errata:
          productName: "RHBQ"
          productVersion: "RHEL-8-RHBQ-2.13"
          productVariant: "8Base-RHBQ-2.13"
```

### Generator configuration

There are three types of configuration options passed to the generator tool:

1. **Static** -- Options that are set always,
2. **Defaults** -- Parameters that are set additionally to the static options in case no custom options provided.
3. **Custom** -- Parameters that are set in the configuration file. These are added to the static options. Defaults are overridden by custom parameters.

#### Domino

Below you can see the static and default parameters set for the Domino generator.

**Static**

```
java -jar domino.jar from-maven report --project-dir=[DIR] --output-file=[OUTPUT_DIR]/bom.json --manifest
```

**Defaults**

```
--include-non-managed --warn-on-missing-scm
```

**Custom**

Run the `java -jar domino.jar from-maven report --help` command to get a list of all possible options for Domino.

#### CycloneDX Maven Plugin

Below you can see the static and default parameters set for the CycloneDX Maven Plugin generator.

**Static**

```
mvn org.cyclonedx:cyclonedx-maven-plugin:[VERSION]:makeAggregateBom -DoutputFormat=json -DoutputName=bom --settings [PATH_TO_SETTINGS_XML_FILE]
```

**Defaults**

```
--batch-mode
```

In case the `verbose` option is not set, additionally following flags are added:

```
--quiet --no-transfer-progress
```

**Custom**

See the [plugin readme](https://github.com/CycloneDX/cyclonedx-maven-plugin) for more
information on what other options can be passed to the tool.

### Processors

Below you can find a list of supported processors and the configuration options for each one.

Please note that this configuration does not influence **which processors are run**. The SBOMer
service configuration controls this. Processor configuration available in the configuration file
influence the processor execution only if the particular processor is enabled in SBOMer.

Currently following processors are run (in order):

1. `default`
2. `redhat-product`

#### `default`

This processor adds available information from the PNC build system into the SBOM.

There are no configuration options for this processor.

#### `redhat-product`

This processor adds Red Hat product information metadata into the main component of the SBOM.

Configuration:

```yaml
redhat-product:
  # Errata tool configuration (optional), if not specified, the PNC-provided data will be used
  errata:
    productName: [ET_PRODUCT_NAME] # required
    productVersion: [ET_PRODUCT_VERSION] # required
    productVariant: [ET_PRODUCT_VARIANT] # required
```

## API

SBOMer exposes a [REST API](api.md) which is documented using Swagger. You can view it at the `/api` endpoint of a deployment.

Ready to view OpenAPI resources are available currently in the staging environment:

- [Swagger UI](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/api/)
- [OpenAPI definition](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/q/openapi) in YAML format.
  The [JSON version is here](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/q/openapi?format=json).

### API versioning

The API is versioned. Current version is `v1alpha1` and it is available at `/api/v1alpha1` context path.

> :warning: **WIP**
>
> Please note that `v1alpha1` has a meaning :) We may change it without a notice.

### Authentication and authorization

Currently there is no authentication or authorization required to use this service. This may change.

### Basic operations

#### Fetching all SBOMs

Please note that the return is a list of SBOMs in a paginated way.

```bash
curl https://sbomer-stage.apps.ocp-c1.prod.psi.redhat.com/api/v1alpha1/sboms
```

#### Requesting SBOM generation manually

> This should not be required, the SBOMer service generates SBOMs automatically for successful PNC builds.

To request an SBOM generation we need to pass the PNC build id:

```bash
curl -v -X POST https://sbomer-stage.apps.ocp-c1.prod.psi.redhat.com/api/v1alpha1/sboms/generate/build/[PNC_BUILD_ID]
```

#### Fetching a specific SBOM

Once the SBOM is generated it becomes available at:

```bash
curl https://sbomer-stage.apps.ocp-c1.prod.psi.redhat.com/api/v1alpha1/sboms/[SBOM_ID]
```
