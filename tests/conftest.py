import os

import pulumi.automation as auto
import pytest
import requests
from tenacity import retry, stop_after_delay, wait_fixed

from polynote_testkit.client import PolyNoteClient

INFRA_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "infra/polynote-ft"))

def pulumi_stack_name() -> str:
    # Make it unique per run if you plan parallel CI runs
    return os.environ.get("PULUMI_STACK", "polynote-ft")

@retry(stop=stop_after_delay(120), wait=wait_fixed(2), reraise=True)
def wait_http_ready(url: str):
    # adjust to a real health endpoint you have (recommended to add /health)
    r = requests.get(url, timeout=3)
    r.raise_for_status()

@pytest.fixture(scope="session")
def polynote_env():
    """
    Brings up Kind + PolyNote via Pulumi, yields endpoints, then destroys infra.
    """
    project_name = "polynote-infra"  # must match Pulumi.yaml name in infra/
    stack_name = pulumi_stack_name()

    # LocalWorkspace runs Pulumi program from an existing directory (infra/)
    stack = auto.create_or_select_stack(
        stack_name=stack_name,
        work_dir=INFRA_DIR,
        project_name=project_name,
    )

    # Optional: set config needed by infra
    # stack.set_config("polynote:replicas", auto.ConfigValue("3"))

    stack.refresh(on_output=print)
    up_res = stack.up(on_output=print)

    outputs = {k: v.value for k, v in up_res.outputs.items()}

    # Expect these outputs to exist
    node_a = outputs["nodeAUrl"]
    node_b = outputs["nodeBUrl"]
    node_c = outputs["nodeCUrl"]

    # Wait until services are reachable
    wait_http_ready(node_a)
    wait_http_ready(node_b)
    wait_http_ready(node_c)

    try:
        yield {
            "node_a": node_a,
            "node_b": node_b,
            "node_c": node_c,
            "outputs": outputs,
            "stack_name": stack_name,
        }
    finally:
        # Always tear down
        stack.destroy(on_output=print)

@pytest.fixture(scope="session")
def node_a(polynote_env):
    return PolyNoteClient(node_id='a', stack_name=polynote_env["stack_name"])

@pytest.fixture(scope="session")
def node_b(polynote_env):
    return PolyNoteClient(node_id='b', stack_name=polynote_env["stack_name"])

@pytest.fixture(scope="session")
def node_c(polynote_env):
    return PolyNoteClient(node_id='c', stack_name=polynote_env["stack_name"])
