import {createPolynoteNode} from "./node";
import {imageName} from "./docker";

const suffixes = ["a", "b", "c"] as const;

const polynoteNodes = Object.fromEntries(
    suffixes.map((suffix) => [suffix, createPolynoteNode(suffix, imageName)])
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
