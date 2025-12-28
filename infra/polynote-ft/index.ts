import {createPolynoteNode} from "../packages/polynote-infra/node";
import * as k8s from "@pulumi/kubernetes";
import {createPolynoteIngress} from "../packages/polynote-infra/ingress";

const suffixes = ["a", "b", "c"] as const;

// todo: namespace should b input from test runner
const namespaceName = "polynote-ft";

new k8s.core.v1.Namespace(namespaceName, {
    metadata: { name: namespaceName },
});

const polynoteNodes = Object.fromEntries(
    suffixes.map((suffix) => [
        suffix,
        createPolynoteNode(suffix, { namespace: namespaceName, volumeType: "emptyDir" }),
    ])
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

export const polynoteIngress = createPolynoteIngress(namespaceName);
