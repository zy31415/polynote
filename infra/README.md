# PolyNote Infra

## Kind cluster setup

The "kind-config.yml" sets up a local Kubernetes cluster with 3 nodes for testing PolyNote.



## Kind cluster dev process

0) Prerequisites
* Node

Plulumi is based on node. I use volta to install node/npm.
`volta install node@20.18`

* Pulumi 

Pulumi is used to build docker image and deploy them into the kind cluster.

To install Pulumi: `brew install pulumi/tap/pulumi`

Login to start use Pulumi: `pulumi login --local`

Create a local .envrc file by copying .envrc.example and set the PULUMI_CONFIG_PASSPHRASE to the value found in 1password.

If you set up pulumi for the first time, run `pulumi install` first.

* Kind

Use Kind to create local k8s cluster for testing.

To deploy changes to the Kind cluster:

1) Edit code
2) Commit changes so that a new sha is generated, i.e. `git rev-parse --short HEAD` is refreshed.
3) Run the following commands to deploy:

```
# Preview changes
pulumi preview

# Apply changes
pulumi up
```
4) After deployment, still need to do port forwarding to access the services. Run the following command in the ../scripts/ directory:

```
./port-forward.sh clean && ./port-forward.sh start
```

5) Check jvm logs in each pod:

```
# Using pod names to get logs
kubectl get pods
kubectl logs -f <pod-name>

# Using label selector to get logs from node A
kubectl logs -f -l node-id=a
```