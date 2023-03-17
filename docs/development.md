# Development

The fastest way to start with SBOMer development is to set up the [development environment](environments.md).

## Running service in development mode

To run the service you just need to run Quarkus in the development mode:

```
./mvnw quarkus:dev -Dquarkus.http.host=0.0.0.0
```

The `-Dquarkus.http.host=0.0.0.0` is required, because we need to expose the REST API port
so that the Tekton Task can reach it from inside of a Kubernetes cluster.

If you are using Minikube with KVM driver, you may want to open the 8080/tcp port:

```
sudo firewall-cmd --zone=libvirt --add-port=8080/tcp --permanent --reload
```

## Tests

```
./mvnw clean verify
```

## Building images

All images used by this service can be found in the `src/main/images` directory.

> :warning: Context path
>
> Please note that in order to build images, the context needs to be set to the **root of the repository**.

There are available scripts that help with building all images:

- `hack/build-images-podman.sh`

  Uses local Podman to build all images.

- `hack/build-images-minikube.sh`

  Build images inside of the Minikube environment. This is very useful in case of the `local` development environment
  where you want to test images in a full Kubernetes deployment.

> :arrow_right: Service needs to be built first!
>
> If you don't use above scripts -- please make sure you build the project before building images to ensure the latest
> state of the service is included in the image.
