import * as docker from "@pulumi/docker";
import * as pulumi from "@pulumi/pulumi";

export function createPolynoteDockerImage(imageName: string, buildContext: string): docker.Image {
    const resolvedSha = (() => {
        try {
            return require("child_process")
                .execSync("git rev-parse --short HEAD")
                .toString()
                .trim();
        } catch {
            return "local";
        }
    })();

    const image = new docker.Image("polynote-image", {
        imageName: pulumi.interpolate`${imageName}:${resolvedSha}`,
        build: { context: buildContext },
        skipPush: true,
    });

    // For local dev, load image into kind cluster
    image.imageName.apply((img) => {
        try {
            require("child_process").execSync(`kind load docker-image ${img} --name polynote`, { stdio: "inherit" });
            pulumi.log.info(`Loaded image ${img} into kind cluster 'polynote'`);
        } catch (err) {
            pulumi.log.warn(`Failed to load image into kind: ${err}`);
        }
        return img;
    });

    return image;
}