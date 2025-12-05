import * as k8s from "@pulumi/kubernetes";
import * as pulumi from "@pulumi/pulumi";

export function createPolynoteNode(suffix: string, imageName: pulumi.Input<string>) {
    const name = `polynote-${suffix}`;
    const labels = { app: "polynote", "node-id": suffix };
    const podName = suffix.toUpperCase();
    const volumePath = `/var/${name}`;
    const volumeName = `${name}-data`;
    const dbPath = `${volumePath}/notes.db`;

    const container = {
        name: "polynote",
        image: imageName,
        imagePullPolicy: "IfNotPresent",
        env: [
            { name: "POD_NAME", value: podName },
            { name: "DB_PATH", value: dbPath },
        ],
        volumeMounts: [{ name: volumeName, mountPath: volumePath }],
    };

    const volumes = [
        {
            name: volumeName,
            hostPath: {
                path: volumePath,
                type: "DirectoryOrCreate",
            },
        },
    ];

    const deployment = new k8s.apps.v1.Deployment(name, {
        metadata: { name },
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
        metadata: { name },
        spec: {
            selector: labels,
            ports: [{ port: 8080, targetPort: 8080 }],
        },
    });

    return { deployment, service };
}
