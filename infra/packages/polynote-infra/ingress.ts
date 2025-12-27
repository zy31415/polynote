import * as k8s from "@pulumi/kubernetes";
import * as pulumi from "@pulumi/pulumi";

export function createPolynoteIngress(namespace: string) {
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

    return new k8s.networking.v1.Ingress("polynote-dev-ingress", {
        metadata: {
            name: "polynote",
            namespace,
            annotations: {
                "nginx.ingress.kubernetes.io/ssl-redirect": "false",
            },
        },
        spec: {
            ingressClassName: "nginx",
            rules,
        },
    });
}
