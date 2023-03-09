# Development and Deployment Environments 101

## Targets, Environments and Profiles

We can identify following **targets**:

- **Local**: Used by engineers to progress quickly with the development. This does not require a Kubernetes cluster at
  all but a container engine (like [Podman](https://podman.io/)) **is still required**.
- **Kubernetes**: A Kubernetes cluster.

  The Kubernetes deployment target can be divided into following sub-targets (overlays):

  - **Local**: This a deployment using a **local Kubernetes cluster**, for example a local
    [Minikube](https://minikube.sigs.k8s.io/docs/) deployment. It can be used by developers to test out locally
    deployment scripts. This installs Tekton as well as other resources. IT assumes that full admin access is available.
    This target should never be used outside of a local Kubernetes deployment.
  - **Production**: This environment is targeted at a remote Kubernetes/OpenShift cluster and is optimized for
    production workload. It assumes that some of the resources are already available in the cluster (for example
    OpenShift Pipelines) and does not require admin access.

  Kubernetes environments are controlled and set up with the [Kustomize](https://kustomize.io/) scripts available in the
  `k8s` directory. Environments mentioned above translate directly into overlays available in the `k8s/overlays/`
  directory.

Besides deployment targets we can identify following Quarkus (application) profiles in use:

- `dev`: Used at development time. Uses a PostgreSQL database deployed on localhost. Used by developers.
- `test`: Used only when tests are executed. This profile is not deployed anywhere. Makes use of the in-memory H2
  database.
- `prod`: The production optimized profile, uses PostgreSQL database which location and credentials are controlled by
  the Kubernetes Secret. This profile is used in staging and production targets.

The team maintains following deployments:

- **Staging** -- Available at: https://sbomer-pct-security-tooling.apps.ocp-c1.prod.psi.redhat.com/ It uses the `prod` profile.
- **Production** -- Not yet created. Will be using the `prod` profile as well.

> **NOTE**
>
> As we are going to target the Continuous Deployment way of delivering software the staging environment may become obsolete or part of a [A/B deployment](https://en.wikipedia.org/wiki/A/B_testing).

### Summary

- If you are a developer, you will be using **local target with dev profile**. You can use Kubernetes development target
  as well if you want to test the service running in a local cluster.
- For staging environment we use staging kubernetes deployment with

- The local target always uses `dev` profile.
- Local Kubernetes development environment uses `dev` profile as well.
- Other Kubernetes environments: staging and production use `prod` profile.

## Local development environment

### JDK 17

This project is developed using JDK 17. Not that any features require it, but why not?

You can use https://sdkman.io/ to install and manage JDKs:

```
sdk install java 17.0.6-tem
```

When you enter the project directory you can run:

```
sdk env
```

And you're all set!

> You can add `$HOME/.sdkman/etc/config` following entry:
>
> ```
> sdkman_auto_env=true
> ```
>
> And have the JDK set automatically when entering the directory.

### Database

WARNING: A container engine is required. Below it is assumed that Podman is available. If you use a different engine,
you need to adjust the command accordingly.

The database container can be started in `detach` mode with the following command:

```
podman run -d --rm \
  -e POSTGRESQL_USER=username \
  -e POSTGRESQL_PASSWORD=password \
  -e POSTGRESQL_DATABASE=sbomer \
  -p 5432:5432 registry.redhat.io/rhel9/postgresql-13@sha256:31fbd226db60cb48ff169491a8b88e69ec727c575ba76dc6095c87f70932b777
```

NOTE: You can authenticate with the registry.redhat.io registry by
[generating a token on this page](https://access.redhat.com/terms-based-registry/#/). You can use a different image, if
you want as well.

Please note that no data will be preserved when you restart the PostgreSQL container. If this is not what you expect you
can add persistence with [volumes](https://docs.podman.io/en/latest/volume.html), for example.

### Run local development environment

To start SBOMer development environment:
```
./mvnw quarkus:dev
```

## Local Kubernetes development environment

### Setting up Minikube

#### Prerequisites

1. Please make sure `kubectl` binary [is installed](https://kubernetes.io/docs/tasks/tools/) as the setup expects kustomize functionality to be available during resources provisioning.
2. `sbomer/k8s/overlays/development/secrets/` directory should contain 2 files:
  * `secret-postgresql.txt` with the following content
  ```
  cat > secret-postgresql.txt << EOF
  POSTGRESQL_USER=username
  POSTGRESQL_PASSWORD=password
  POSTGRESQL_DATABASE=sbomer
  EOF
  ```
  * `secret-redhatio.json` with `${XDG_RUNTIME_DIR}/containers/auth.json` or `$HOME/.docker/config.json` file contents. Please auth against registry.redhat.io first.

This environment makes use of [Minikube](https://minikube.sigs.k8s.io/docs/).

```
minikube start \
  --driver=kvm2 \
  --cpus=4 \
  --memory=6g \
  --disk-size=30GB \
  --kubernetes-version=v1.23.16 \
  --embed-certs \
  -p sbomer
```

NOTE: If you use a different target (container engine, virtual machine manager) than KVM you need to adjust above
command.

### Deploying SBOMer

To deploy SBOMer you just need to run following command:

```
kubectl apply -k k8s/overlays/development/
```

This command deploys everything that is required to run SBOMer in a Kubernetes environment. This includes Tekton as well
as the PostgreSQL database and all the deployments.

NOTE: In a fresh environment it may be necessary to run the command again after a while. The reason is that it registers
Tekton resources but the Kubernetes cluster may not know this type of resources yet, because these are deployed at the
same time.

After short tme you should see SBOMer running:

```
> kubectl get deployments.apps sbomer
NAME     READY   UP-TO-DATE   AVAILABLE   AGE
sbomer   3/3     3            3           108s
```

### Accessing the SBOMer service

You can forward the port to your local machine to access SBOMer service:

```
kubectl port-forward services/sbomer 8080:80
```

This makes it available at: http://localhost:8080.

### Starting from scratch

You can always start from scratch, just remove the `sbomer` profile:

```
minikube delete -p sbomer
```

## Kubernetes staging environment

To deploy the service to staging environment, you need to log in first to the https://console-openshift-console.apps.ocp-c1.prod.psi.redhat.com/k8s/cluster/projects/pct-security-tooling project. You can run afterwards:

```
kubectl apply -k k8s/overlays/production/
```

## Kubernetes production environment

TBD
