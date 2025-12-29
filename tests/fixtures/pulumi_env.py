import logging
import os

import pulumi.automation as auto
import pytest

from tests.support.waiters import wait_http_ready

logger = logging.getLogger(__name__)

INFRA_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "infra/polynote-ft"))

def pulumi_stack_name() -> str:
    return os.environ.get("PULUMI_STACK", "polynote-ft")

def pytest_addoption(parser):
    parser.addoption(
        "--cluster-teardown",
        action="store",
        default="always",
        choices=["never", "on-success", "always"],
        help="Cluster teardown behavior",
    )

@pytest.fixture(scope="session")
def cluster_teardown_mode(request) -> str:
    return request.config.getoption("--cluster-teardown")

@pytest.fixture(scope="session")
def polynote_env(request, cluster_teardown_mode, node_ids):
    """
    Brings up Kind + PolyNote via Pulumi, yields stack info, then (optionally) destroys infra.
    """
    project_name = "polynote-infra"
    stack_name = pulumi_stack_name()
    stack = None

    logger.info(
        "Pulumi stack_name=%s. Cluster teardown mode=%s. create/select stack ...",
        stack_name,
        cluster_teardown_mode,
    )

    try:
        stack = auto.create_or_select_stack(
            stack_name=stack_name,
            work_dir=INFRA_DIR,
            project_name=project_name,
        )

        stack.refresh(on_output=None, on_error=print)
        up_res = stack.up(on_output=None, on_error=print)

        outputs = {k: v.value for k, v in up_res.outputs.items()}

        # Validate expected outputs
        for node_id in node_ids:
            assert node_id in outputs["deploymentNames"]
            assert node_id in outputs["serviceNames"]

        logger.info("Pulumi stack_name=%s is up.", stack_name)

        for node_id in node_ids:
            wait_http_ready(node_id, stack_name)
            logger.info("Node %s is reachable.", node_id)

        yield {"outputs": outputs, "stack_name": stack_name}

    finally:
        if stack is None:
            return

        if cluster_teardown_mode == "never":
            logger.warning("Cluster teardown mode=never. Keeping Pulumi stack %s.", stack_name)
            return

        if cluster_teardown_mode == "on-success" and request.session.testsfailed > 0:
            logger.warning(
                "Tests failed (%s). Keeping Pulumi stack %s for debugging.",
                request.session.testsfailed,
                stack_name,
            )
            return

        logger.info("Tearing down Pulumi stack %s ...", stack_name)
        stack.destroy(on_output=print)
        logger.info("Teardown complete.")