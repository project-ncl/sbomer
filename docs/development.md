# Development

The fastest way to start with SBOMer development is to set up the [local development environment](environments.md).

## Running service in development mode

To run the service you just need to run Quarkus in the development mode:

```
./mvnw quarkus:dev
```

## Tests

```
./mvnw clean package
```

## Building images

There are two images this service uses:

- Image containing the service itself, and
- Image containing all necessary tools to execute the SBOM generation within the Tekton environment.

### Building service image

First image can be built using Quarkus itself:

```
./mvnw clean package -Dquarkus.container-image.build=true
```

This uses the default builder configured (which is [Jib](https://github.com/GoogleContainerTools/jib)).

> **NOTE**
>
> You can read more about building container images in the [Quarkus documentation](https://quarkus.io/guides/container-image). You can use the `-Dquarkus.container-image.builder` to switch to a different builder, for example: `-Dquarkus.container-image.builder=docker`. This covers Docker and Podman (see [this page](https://quarkus.io/blog/quarkus-devservices-testcontainers-podman/) for Podman setup).

### Building generator image

```
podman build -f src/main/docker/generator/Containerfile -t quay.io/goldmann/sbomer-generator:latest
```