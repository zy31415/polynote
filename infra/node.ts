import * as k8s from "@pulumi/kubernetes";

export function createPolynoteNode(suffix: string) {
    const labels = {
        app: "polynote",
        "node-id": suffix
    };

    const name = `polynote-${suffix}`;
    const podName = `${suffix}`.toUpperCase();
    
    const volumnPath = `/var/polynote-${suffix}`;
    const dbPath = `${volumnPath}/notes.db`;
    const volumeName = `polynote-${suffix}-data`;

    const deployment = new k8s.apps.v1.Deployment(name, {
        metadata: {
            name: name,
        },
        spec: {
            replicas: 1,
            selector: {
                matchLabels: labels,
            },
            template: {
                metadata: {
                    labels,
                },
                spec: {
                    containers: [
                        {
                            name: "polynote",
                            image: "polynote:latest",
                            imagePullPolicy: "IfNotPresent",
                            env: [
                                {
                                    name: "POD_NAME",
                                    value: podName,
                                },
                                {
                                    name: "DB_PATH",
                                    value: dbPath,
                                },
                            ],
                            volumeMounts: [
                                {
                                    name: volumeName,
                                    mountPath: volumnPath,
                                },
                            ],
                        },
                    ],
                    affinity: {
                        nodeAffinity: {
                            requiredDuringSchedulingIgnoredDuringExecution: {
                                nodeSelectorTerms: [
                                    {
                                        matchExpressions: [
                                            {
                                                key: "role",
                                                operator: "In",
                                                values: [name],
                                            },
                                        ],
                                    },
                                ],
                            },
                        },
                    },
                    volumes: [
                        {
                            name: volumeName,
                            hostPath: {
                                path: volumnPath,
                                type: "DirectoryOrCreate",
                            },
                        },
                    ],
                },
            },
        },
    });

    const service = new k8s.core.v1.Service(name, {
        metadata: {
            name: name,
        },
        spec: {
            selector: labels,
            ports: [
                {
                    port: 8080,
                    targetPort: 8080,
                },
            ],
        },
    });

    return { deployment, service };
}