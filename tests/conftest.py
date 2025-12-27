import logging
import os

import pulumi.automation as auto
import pytest
import requests
from polynote_testkit import PolyNoteClient, FTConfig
from tenacity import retry, stop_after_delay, wait_fixed

logger = logging.getLogger(__name__)

INFRA_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "infra/polynote-ft"))

def pulumi_stack_name() -> str:
    # Make it unique per run if you plan parallel CI runs
    return os.environ.get("PULUMI_STACK", "polynote-ft")

@retry(stop=stop_after_delay(120), wait=wait_fixed(2), reraise=True)
def wait_http_ready(node_id: str, stack_name: str):
    # adjust to a real health endpoint you have (recommended to add /health)
    headers = {"Host": f"{node_id}.{stack_name}.polynote.local"}
    r = requests.get(FTConfig.HEALTH_CHECK_URL, headers=headers, timeout=3)
    r.raise_for_status()

@pytest.fixture(scope="session")
def polynote_env(request):
    """
    Brings up Kind + PolyNote via Pulumi, yields endpoints, then destroys infra.
    """
    project_name = "polynote-infra"  # must match Pulumi.yaml name in infra/
    stack_name = pulumi_stack_name()
    logger.info(f"For this test run, Pulumi stack_name={stack_name}. Starting create/select stack ...")

    try:
        # LocalWorkspace runs Pulumi program from an existing directory (infra/)
        stack = auto.create_or_select_stack(
            stack_name=stack_name,
            work_dir=INFRA_DIR,
            project_name=project_name,
        )

        # Optional: set config needed by infra
        # stack.set_config("polynote:replicas", auto.ConfigValue("3"))

        stack.refresh(on_output=None, on_error=print)
        up_res = stack.up(on_output=None, on_error=print)

        outputs = {k: v.value for k, v in up_res.outputs.items()}

        # Expect node_id in deploymentNames and serviceNames
        for node_id in ['a', 'b', 'c']:
            assert node_id in outputs["deploymentNames"] and node_id in outputs["serviceNames"]

        logger.info(f"Pulumi stack_name={stack_name} is up.")

        # Wait until services are reachable
        for node_id in ['a', 'b', 'c']:
            wait_http_ready(node_id, stack_name)
            logger.info(f"Node {node_id} is reachable.")

        logger.info(f"Deployment is ready for testing.")

        yield {
            "outputs": outputs,
            "stack_name": stack_name,
        }
    finally:
        if stack is None:
            return

        # pytest sets this after tests run; teardown runs after yield
        if request.session.testsfailed:
            logger.warning(
                f"Tests failed ({request.session.testsfailed}). "
                f"Keeping Pulumi stack {stack_name} for debugging."
            )
            return

        logger.info(f"Tearing down Pulumi stack name={stack_name} ...")
        stack.destroy(on_output=print)
        logger.info("Teardown complete.")

@pytest.fixture(scope="session")
def node_a(polynote_env):
    return PolyNoteClient(node_id='a', stack_name=polynote_env["stack_name"])

@pytest.fixture(scope="session")
def node_b(polynote_env):
    return PolyNoteClient(node_id='b', stack_name=polynote_env["stack_name"])

@pytest.fixture(scope="session")
def node_c(polynote_env):
    return PolyNoteClient(node_id='c', stack_name=polynote_env["stack_name"])
