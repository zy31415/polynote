# Kind Cluster

This directory contains configuration and scripts to set up a local Kubernetes cluster using Kind for dev/testing PolyNote.

## Kind Cluster Setup

Kind cluster setup uses yml config file. Run the following command to create the kind cluster:

```shell
./setup_kind_cluster.sh
```

## K8s ingress setup
Ingress is managed via Pulumi.

After the kind cluster is created, run the following script to set up the ingress controller:

```shell
pulumi preview
pulumi up 
```
