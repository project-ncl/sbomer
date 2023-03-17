# Development and Deployment Environments 101

## Environments and Profiles

We can identify following deployment **environments**:

- `development`
- `local`
- `staging`
- `production`

These strictly correlate to Kustomize overlays we have in the `k8s/overlays/` directory.

Every environment requires a Kubernetes cluster. For example `development` and `local` can use Minikube.
We use OpenShift for `staging` and `development`.

### Development environment

The `development` environment is the most developer friendly. It hosts all the workloads (Tekton) inside
a Minikube cluster, but the service itself is run outside of the cluster. It can be run for example in an IDE,
even in debug mode while still being able to schedule and react on Tekton PipelineRuns.

:arrow_right: Please see the [development docs](development.md) on how to use it.

### Local environment

There is a difference between the `local` and `development` environment. In the `local` environment we run
everything (including the service) inside a Kubernetes cluster. This environment can be used to quickly
deploy everything into a _local_ Kubernetes cluster.


### Staging environment

We deploy to staging on every push to the `main` branch.

### Production environment

TBD
