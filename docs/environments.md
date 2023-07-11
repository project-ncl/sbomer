# Deployment Environments

Our deployment environments are maintained via Helm chart.

## Environments

We can identify following deployment **environments**:

- `development`
- `staging`
- `production`

Every environment requires a Kubernetes cluster. For example `development` can use Minikube.
We use OpenShift for `staging` and `production`.

### Development environment

The `development` environment is the most developer friendly. It hosts all the workloads (Tekton) inside
a Minikube cluster, but the service itself is run outside of the cluster. It can be run for example in an IDE,
even in debug mode while still being able to schedule and react on Tekton PipelineRuns.

:arrow_right: Please see the [development docs](development.md) on how to use it.

### Staging environment

We deploy to staging on every push to the `main` branch.

### Production environment

Production deployment is the mirror of staging deployment. Deployment to production is
currently triggered manually, once a set of tests on the staging environment is performed
and we are sure things are in correct state.

We deploy to production daily.