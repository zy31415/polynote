# PolyNote Infra

## Kind cluster setup

The "kind-config.yml" sets up a local Kubernetes cluster with 3 nodes for testing PolyNote.


## K8s setup

K8s setup is managed by Pulumi. See `Pulumi.yaml` and `index.ts` for details.

```
# Preview changes
pulumi preview

# Apply changes
pulumi up
```