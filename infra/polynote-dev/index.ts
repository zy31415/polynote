import {createPolynoteNode} from "./node";
import {imageName} from "./docker";
import * as k8s from "@pulumi/kubernetes";
import "./ingress";

const suffixes = ["a", "b", "c"] as const;
const namespaceName = "polynote-dev";

new k8s.core.v1.Namespace(namespaceName, {
    metadata: { name: namespaceName },
});

const polynoteNodes = Object.fromEntries(
    suffixes.map((suffix) => [suffix, createPolynoteNode(suffix, imageName, namespaceName)])
) as Record<(typeof suffixes)[number], ReturnType<typeof createPolynoteNode>>;

export const deploymentNames = Object.fromEntries(
    Object.entries(polynoteNodes).map(([suffix, { deployment }]) => [
        suffix,
        deployment.metadata.name,
    ])
);

export const serviceNames = Object.fromEntries(
    Object.entries(polynoteNodes).map(([suffix, { service }]) => [
        suffix,
        service.metadata.name,
    ])
);
