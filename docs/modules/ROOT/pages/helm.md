# Helm

We are using [Helm](https://helm.sh/) to manage our [deployments](deployments.md). Below you can find some
helpful commands that can be used while developing and deploying SBOMer using Helm.

## Installing Helm

You need to [install Helm](https://helm.sh/docs/intro/install/). Use latest version. You can use downloadable release
or the version that comes with your operating system.

WARNING: Before you run any Helm command ensure you **use the correct context**! In other words -- make sure
you are running it against the correct Kubernetes deployment. You can use `kubectl config` commands to manage
your contexts. And then specify `--kube-context sbomer-local` to explicitly use one where `sbomer-local` is the
name of the context. This ensures that you do not make any unexpected changes.

## Cheat sheet

### Upgrade (or install)

https://helm.sh/docs/helm/helm_upgrade/

Upgrade (or install, if not found) a development version of SBOMer (for example on a Minikube [development environment](development.md)).

TIP: You can use `--debug` switch to print more information as well as the generated manifest.

The `sbomer` part of the command is the deployment name. It is possible to deploy it under a different name
and maintain blue/green deployments, but SBOMer is not prepared yet for such deployment. Stick to `sbomer.`

This command can be run multiple times. Every time it is run, it will bump the deployment revision.

```bash
$ helm --kube-context sbomer-local upgrade --install sbomer ./helm
Release "sbomer" has been upgraded. Happy Helming!
NAME: sbomer
LAST DEPLOYED: Mon Jul 10 09:59:41 2023
NAMESPACE: default
STATUS: deployed
REVISION: 2
TEST SUITE: None
```

### Release history

https://helm.sh/docs/helm/helm_history/

Get the revision history.

```bash
$ helm --kube-context sbomer-local history sbomer
REVISION	UPDATED                 	STATUS    	CHART       	APP VERSION	DESCRIPTION
1       	Mon Jul 10 09:55:58 2023	superseded	sbomer-0.1.0	1.0.0      	Install complete
2       	Mon Jul 10 09:59:41 2023	deployed  	sbomer-0.1.0	1.0.0      	Upgrade complete
```

### Manifest

https://helm.sh/docs/helm/helm_get_manifest/

Get a manifest (all Helm managed resources) in a YAML format for latest revision.

```bash
helm --kube-context sbomer-local get manifest sbomer
```

Similarly you can obtain manifest for production deployment (assuming you have the `sbomer-prod` context set properly)

```bash
helm --kube-context sbomer-prod get manifest sbomer
```

### Uninstall

https://helm.sh/docs/helm/helm_uninstall/

Uninstall application. This will **remove the application from the cluster, without asking!**

```bash
helm --kube-context sbomer-local uninstall sbomer
```

### Rollback

https://helm.sh/docs/helm/helm_rollback/

Return to previous revision.

```bash
$ helm --kube-context sbomer-local rollback sbomer 1
Rollback was a success! Happy Helming!
$ helm --kube-context sbomer-local history sbomer
REVISION	UPDATED                 	STATUS    	CHART       	APP VERSION	DESCRIPTION
1       	Mon Jul 10 09:55:58 2023	superseded	sbomer-0.1.0	1.0.0      	Install complete
2       	Mon Jul 10 09:59:41 2023	superseded	sbomer-0.1.0	1.0.0      	Upgrade complete
3       	Mon Jul 10 10:13:12 2023	deployed  	sbomer-0.1.0	1.0.0      	Rollback to 1
```

### Rendering templates

https://helm.sh/docs/helm/helm_template/

You can render template without applying (installing) it to a new release. This will generate the YAML
descriptors for the release and print them to standard output.

```bash
$ helm template sbomer ./helm/
```

This is similar to the `helm get manifest` command with the difference that `template` renders
from the source code whereas the `get manifest` returns installed descriptors.

#### Using different environments (stage, prod)

By default descriptors for the development version are generated. To target different environments you
need to specify an override file, like this:

```bash
$ helm template --values ./helm/env/prod.yaml sbomer ./helm/
```

This is what our CI does automatically for staging and production deployments.