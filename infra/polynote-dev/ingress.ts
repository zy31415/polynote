import * as k8s from "@pulumi/kubernetes";
import * as pulumi from "@pulumi/pulumi";

const namespace = "polynote-dev";

// Assumes Services already exist with these names:
const servicePort = 8080;

const env = pulumi.getStack(); // "dev" or "ft"
const baseDomain = "polynote.local";

const instances = ["a", "b", "c"];

const rules = instances.map((id) => ({
    host: `${id}.${env}.${baseDomain}`,
    http: {
        paths: [
            {
                path: "/",
                pathType: "Prefix",
                backend: {
                    service: { name: `polynote-${id}`, port: { number: servicePort } },
                },
            },
        ],
    },
}));

export const polynoteIngress = new k8s.networking.v1.Ingress("polynote-dev-ingress", {
    metadata: {
        name: "polynote",
        namespace,
        annotations: {
            // optional but sometimes helpful:
            "nginx.ingress.kubernetes.io/ssl-redirect": "false",
        },
    },
    spec: {
        ingressClassName: "nginx",
        rules,
    },
});