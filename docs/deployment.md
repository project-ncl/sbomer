# Staging and production deployments

## Workflow

Our CI is automatically building the service when a new commit is detected in the `main` branch.
This includes the service and all related images as well. Images are pushed to quay.io registry.

Once this is done, the automatic deployment to staging begins. This deployment uses the Recreate strategy.
For staging it is good enough -- we can allow for some small downtime in favor of lowering the complexity
of the set up.

Currently same strategy is used for production, but this will be changed in the future.

