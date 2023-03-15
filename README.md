# SBOMer

A service to generate SBOMs in the CycloneDX format for Project Newcastle (PNC) builds.

## Documentation

See [the documentation](docs/index.md).

## Kubernetes deployment

Kustomize deployment scripts are available:

```
kubectl apply -k k8s/overlays/production
```

This will run the service using **published** images.
