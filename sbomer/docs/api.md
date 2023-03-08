# SBOMer API

The API is documented using Swagger. You can view it at the `/api` endpoint of a deployment.

Ready to view OpenAPI resources are available currently in the staging environment:

- [Swagger UI](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/api/)
- [OpenAPI definition](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/q/openapi) in YAML format.
  The [JSON version is here](https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/q/openapi?format=json).

Locally you can run the application in development mode and head to http://localhost:8080/api.

```
./mvnw quarkus:dev
```

## Versioning

The API is versioned. Current version is `v1` and it is available at `/api/v1` context path.
