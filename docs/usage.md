# SBOMer Usage

> :warning: **WIP**
>
> This service is under heavy development and refactoring currently.

## API

SBOMer exposes a [REST API](api.md) which is documented using Swagger. You can view it at the `/api` endpoint of a deployment.

Ready to view OpenAPI resources are available currently in the staging environment:

- [Swagger UI](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/api/)
- [OpenAPI definition](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/q/openapi) in YAML format.
  The [JSON version is here](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/q/openapi?format=json).

### API versioning

The API is versioned. Current version is `v1beta1` and it is available at `/api/v1beta1` context path. 

> :warning: **WIP**
>
> Please note that `v1beta1` has a meaning :) We may change it without a notice.

## Authentication and authorization

Currently there is no authentication or authorization required to use this service. This may change.

## Basic operations

### Fetching all SBOMs

Please note that the return is a list of SBOMs in a paginated way.

```bash
curl https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/api/v1beta1/sboms
```

### Requesting SBOM generation

To request an SBOM generation we need to pass the PNC build id:

```bash
curl -v -X POST https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/api/v1beta1/sboms/generate/build/[PNC_BUILD_ID]
```

Please note that we currently do not provide a reference in the returned message. This will be improved.

### Fetching SBOM for a specific PNC build id

Once the SBOM is generated it becomes available at:

```bash
curl https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/api/v1beta1/sboms?query=buildId=eq=[PNC_BUILD_ID]
```
