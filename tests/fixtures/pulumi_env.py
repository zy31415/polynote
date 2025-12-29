import logging
import os

import pytest

from ..polynote_testkit.pulumi import PulumiPolynoteRunner

logger = logging.getLogger(__name__)

INFRA_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "infra/polynote-ft"))


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
    Pytest wrapper around PulumiPolynoteRunner.
    """
    runner = PulumiPolynoteRunner(
        project_name="polynote-infra",
        work_dir=INFRA_DIR,
        node_ids=node_ids,
    )

    logger.info("Cluster teardown mode=%s", cluster_teardown_mode)

    env = None
    try:
        env = runner.up()
        yield {"outputs": env.outputs, "stack_name": env.stack_name}
    finally:
        # teardown policy is pytest-specific, so it stays here
        if cluster_teardown_mode == "never":
            logger.warning("Cluster teardown mode=never. Keeping Pulumi stack %s.", runner.stack_name)
            return

        if cluster_teardown_mode == "on-success" and request.session.testsfailed > 0:
            logger.warning(
                "Tests failed (%s). Keeping Pulumi stack %s for debugging.",
                request.session.testsfailed,
                runner.stack_name,
            )
            return

        runner.destroy()
