# Staging and production deployment

## Workflow

Our CI is automatically building the service when a new commit is detected in the `main` branch.
This includes the service and all related images as well. Images are pushed to quay.io registry.

Once this is done, the automatic deployment to staging begins. This deployment uses the Recreate strategy.
For staging it is good enough -- we can allow for some small downtime in favor of lowering the complexity
of the set up.

TBD Production.

## Service Account

In order to deploy the service (apply the Kustomize output) we need to have a dedicated Service Account
which can be used to authenticate against the target cluster. We need such a Service Account for every
target environment.

```
oc create sa sbomer-deployer
```

### Required RoleBindings

By default no additional RoleBindings are required.

### Getting the `sbomer-deployer` Service Account token

Because it is fairly unintuitive to retrieve the token from a given Service Account
by just utilizing the Kubernetes CLI, we will use the `jq` command:

```
oc get secret -o json | jq -r '[.items[] | select (.metadata.annotations["kubernetes.io/service-account.name"]=="sbomer-deployer") | select (.type=="kubernetes.io/service-account-token")][0] | .data.token' | base64 -d
```