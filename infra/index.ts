import {createPolynoteNode} from "./node";

const nodeA = createPolynoteNode("a");
export const deploymentNameNodeA = nodeA.deployment.metadata.name;
export const serviceNameNodeA = nodeA.service.metadata.name;

const nodeB = createPolynoteNode("b");
export const deploymentNameNodeB = nodeB.deployment.metadata.name;
export const serviceNameNodeB = nodeB.service.metadata.name;

const nodeC = createPolynoteNode("c");
export const deploymentNameNodeC = nodeC.deployment.metadata.name;
export const serviceNameNodeC = nodeC.service.metadata.name;
