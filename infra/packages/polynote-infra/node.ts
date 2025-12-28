import * as k8s from "@pulumi/kubernetes";
import * as pulumi from "@pulumi/pulumi";
import { createPolynoteDockerImage } from "./docker";

const DEFAULT_IMAGE_NAME = "polynote";
const DEFAULT_BUILD_CONTEXT = "../../";

let polynoteImage: ReturnType<typeof createPolynoteDockerImage> | undefined;

function getPolynoteImageName(): pulumi.Input<string> {
    if (!polynoteImage) {
        polynoteImage = createPolynoteDockerImage(DEFAULT_IMAGE_NAME, DEFAULT_BUILD_CONTEXT);
    }

    return polynoteImage.imageName;
}

type PolynoteNodeVolumeType = "hostPath" | "emptyDir";

type PolynoteNodeOptions = {
    namespace?: pulumi.Input<string>;
    volumeType?: PolynoteNodeVolumeType;
};

// todo: refactor these code - this function can be broken down.
export function createPolynoteNode(suffix: string, namespace?: pulumi.Input<string>): {
    deployment: k8s.apps.v1.Deployment;
    service: k8s.core.v1.Service;
};
export function createPolynoteNode(
    suffix: string,
    options?: PolynoteNodeOptions
): {
    deployment: k8s.apps.v1.Deployment;
    service: k8s.core.v1.Service;
};
export function createPolynoteNode(
    suffix: string,
    namespaceOrOptions?: pulumi.Input<string> | PolynoteNodeOptions,
    options?: PolynoteNodeOptions
) {
    const looksLikeOptions =
        namespaceOrOptions !== undefined &&
        typeof namespaceOrOptions === "object" &&
        ("volumeType" in namespaceOrOptions || "namespace" in namespaceOrOptions);
    const resolvedOptions: PolynoteNodeOptions = looksLikeOptions
        ? namespaceOrOptions
        : { ...options, namespace: namespaceOrOptions as pulumi.Input<string> | undefined };
    const { namespace, volumeType = "hostPath" } = resolvedOptions;
    const name = `polynote-${suffix}`;
    const labels = { app: "polynote", "node-id": suffix };
    const podName = suffix.toUpperCase() // pod name uses uppercase suffix
    const volumePath = `/var/${name}`;
    const volumeName = `${name}-data`;
    const dbPath = `${volumePath}/notes.db`;

    const container = {
        name: "polynote",
        image: getPolynoteImageName(),
        imagePullPolicy: "IfNotPresent",
        env: [
            { name: "POD_NAME", value: podName },
            { name: "DB_PATH", value: dbPath },
        ],
        volumeMounts: [{ name: volumeName, mountPath: volumePath }],
    };

    const volumes = [
        volumeType === "hostPath"
            ? {
                name: volumeName,
                hostPath: {
                    path: volumePath,
                    type: "DirectoryOrCreate",
                },
            }
            : {
                name: volumeName,
                emptyDir: {},
            },
    ];

    const deployment = new k8s.apps.v1.Deployment(name, {
        metadata: { name, namespace },
        spec: {
            replicas: 1,
            selector: { matchLabels: labels },
            template: {
                metadata: { labels },
                spec: {
                    containers: [container],
                    affinity: {
                        nodeAffinity: {
                            requiredDuringSchedulingIgnoredDuringExecution: {
                                nodeSelectorTerms: [
                                    {
                                        matchExpressions: [
                                            { key: "role", operator: "In", values: [name] },
                                        ],
                                    },
                                ],
                            },
                        },
                    },
                    volumes,
                },
            },
        },
    });

    const service = new k8s.core.v1.Service(name, {
        metadata: { name, namespace },
        spec: {
            selector: labels,
            ports: [{ port: 8080, targetPort: 8080 }],
        },
    });

    return { deployment, service };
}
