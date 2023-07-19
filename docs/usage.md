# SBOMer usage

## How it works?

Once a build in Project Newcastle build system (PNC) finishes SBOMer receives a notification about this
fact and the Software Bill of Materials (SBOM) generation process is started automatically.

Generated SBOM is stored in the SBOMer database and a notification
is sent on the UMB so that other systems will be aware of it.

SBOMs can be fetched from the SBOMer using the REST API.

Currently only Maven projects are supported.

## Configuration

Although SBOMer was designed in a way it should not require user interaction to generate SBOMs,
there is a requirement to ensure the relationship between the generated SBOM and a particular product
release is maintained.

After suggestion from PST a decision has been made to use the Errata Tool identifiers of the product to
identify the product release for a particular SBOM. This data needs to be provided to SBOMer in some way.

There are currently two options:

1. Update the internal [SBOMer mapping file](cli/src/main/resources/mapping/production/product-mapping.yaml),
2. Provide required data in a configuration file.

SBOMer configuration file, besides providing the product mapping, can adjust the generation process as well.
In most cases defaults will be a good fit, but sometimes changes will be required.

In any case, the resulting configuration (user provided + defaults) is validated against schema to ensure
a valid configuration.

NOTE: If you are unsure about what settings should be used, don't hesitate to contact SBOMer developers!

Below you can see an example configuration file.

```yaml
# Optional, can be omitted. If not provided -- latest version will be assumed.
apiVersion: sbomer.jboss.org/v1alpha1
# Array of products
products:
  - processors:
      # Custom processors configuration (optional)
      # Please note that this is an array!

      # Specific processor configuration
      # All processors and available configs are listed below
      - processors:
          - type: redhat-product
            errata:
              productName: "RHTESTPRODUCT"
              productVersion: "RHEL-8-RHTESTPRODUCT-1.1"
              productVariant: "8Base-RHTESTPRODUCT-1.1"

    generator:
      # Custom generator configuration (optional)

      type: maven-cyclonedx

      # Additional arguments passed to the generator.
      args: "--batch-mode"

      # Version of the generator.
      version: 2.7.9
```

Please note that the `products` element is an array! This makes it possible to cover the unusual
case where a single build results in multiple product releases.

This file should be stored in the Gerrit repository that is used by the PNC Build Configuration under
the `.sbomer/config.yaml` path.

### Examples

Example of support for multi-product source code repository where a single build (configuration)
in PNC can build more than one product. In the example below have two products defined.

```yaml
apiVersion: sbomer.jboss.org/v1alpha1
products:
  - processors:
      - type: redhat-product
        errata:
          productName: "CCCDDD"
          productVersion: "CCDD"
          productVariant: "CD"
    generator:
      type: maven-domino
      args: "--config-file .domino/cccddd.json --warn-on-missing-scm"
      version: "0.0.90"

  - processors:
      - type: redhat-product
        errata:
          productName: "AAABBB"
          productVersion: "AABB"
          productVariant: "AB"
    generator:
      type: maven-domino
      customArgs: "--config-file .domino/aaabbb.json --warn-on-missing-scm"
```

A single product (most common use case) where only the required configuration for the
`redhat-product` processor is provided. For everything else default values are used.

```yaml
apiVersion: sbomer.jboss.org/v1alpha1
products:
  - processors:
      - type: redhat-product
        errata:
          productName: "RHBQ"
          productVersion: "RHEL-8-RHBQ-2.13"
          productVariant: "8Base-RHBQ-2.13"
```

### Generator configuration

There are three types of configuration options passed to the generator tool:

1. **Static** -- Options that are set always,
2. **Defaults** -- Parameters that are set additionally to the static options in case no custom options are provided.
3. **Custom** -- Parameters that are set in the configuration file. These are added to the static options. These options override defaults mentioned above.

Currently **default generator** is the CycloneDX Maven Plugin generator.

#### Domino Maven generator

* Type: `maven-domino`
* Default version: 0.0.90.

Below you can see the static and default parameters set for the Domino generator.

**Static**

```
java -jar domino.jar report --project-dir=[DIR] --output-file=[OUTPUT_DIR]/bom.json --manifest -s [PATH_TO_SETTINGS_XML_FILE]
```

**Defaults**

```
--include-non-managed --warn-on-missing-scm
```

**Custom arguments**

Run the `java -jar domino.jar report --help` command to get a list of all possible options for Domino.

#### CycloneDX Maven Plugin generator

* Type: `maven-cyclonedx`
* Default version: 2.7.9.

Below you can see the static and default parameters set for the CycloneDX Maven Plugin generator.

**Static**

```
mvn org.cyclonedx:cyclonedx-maven-plugin:[VERSION]:makeAggregateBom -DoutputFormat=json -DoutputName=bom --settings [PATH_TO_SETTINGS_XML_FILE]
```

**Defaults**

```
--batch-mode
```

**Custom arguments**

See the [plugin readme](https://github.com/CycloneDX/cyclonedx-maven-plugin) for more
information on what other options can be passed to the tool.

### Processors

Below you can find a list of supported processors and the configuration options for each one.

Please note that this configuration does not influence **which processors are run**. The SBOMer
service configuration controls this if. If there are missing processors, these will be added.

Currently following processors are required to run (in order):

1. `default`
2. `redhat-product`

#### `default`

This processor adds available information from the PNC build system into the SBOM.

There are no configuration options for this processor.

#### `redhat-product`

This processor adds Red Hat product information metadata into the main component of the SBOM.

Configuration:

```yaml
type: redhat-product:
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

### Basic SBOM operations

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

### Generation requests

Generation requests provide information on the progress of the generation.

#### Fetching all generation requests

List of all requests:

```bash
curl https://sbomer-stage.apps.ocp-c1.prod.psi.redhat.com/api/v1alpha1/sboms/requests
```

#### Fetching a specific generation request

Get a specific generation request:

```bash
curl https://sbomer-stage.apps.ocp-c1.prod.psi.redhat.com/api/v1alpha1/sboms/requests/[REQUEST_ID]
```
