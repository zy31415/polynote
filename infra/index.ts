import * as k8s from "@pulumi/kubernetes";

const labels = {
    app: "polynote",
    "node-id": "a",
};

const deployment = new k8s.apps.v1.Deployment("polynote-a", {
    metadata: {
        name: "polynote-a",
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
                                value: "A",
                            },
                            {
                                name: "DB_PATH",
                                value: "/var/polynote-a/notes.db",
                            },
                        ],
                        volumeMounts: [
                            {
                                name: "polynote-a-data",
                                mountPath: "/var/polynote-a",
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
                                            values: ["polynote-a"],
                                        },
                                    ],
                                },
                            ],
                        },
                    },
                },
                volumes: [
                    {
                        name: "polynote-a-data",
                        hostPath: {
                            path: "/var/polynote-a",
                            type: "DirectoryOrCreate",
                        },
                    },
                ],
            },
        },
    },
});

const service = new k8s.core.v1.Service("polynote-a", {
    metadata: {
        name: "polynote-a",
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

export const deploymentName = deployment.metadata.name;
export const serviceName = service.metadata.name;
