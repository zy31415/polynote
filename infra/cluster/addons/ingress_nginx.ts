import * as k8s from "@pulumi/kubernetes";

// todo: study: what is ingress exactly? How is it different from load balancer service?
// todo: Should I use just yaml and no pulumi for this?
export function installIngressNginx() {
    const ingressNs = new k8s.core.v1.Namespace("ingress-nginx-ns", {
        metadata: { name: "ingress-nginx" },
    });

    const ingress = new k8s.helm.v3.Release(
        "ingress-nginx",
        {
            chart: "ingress-nginx",
            namespace: ingressNs.metadata.name,
            repositoryOpts: {
                repo: "https://kubernetes.github.io/ingress-nginx",
            },
            values: {
                controller: {
                    service: {
                        type: "NodePort", // good default for kind
                        nodePorts: {
                            http: 30807,
                            https: 30806,
                        }
                    },
                },
            },
        },
        { dependsOn: ingressNs },
    );

    return { ingressNs, ingress };
}
