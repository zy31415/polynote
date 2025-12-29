from __future__ import annotations

import logging
import os
from dataclasses import dataclass
from typing import Any, Dict, Iterable, Optional

import pulumi.automation as auto
from polynote_testkit.waiters import wait_http_ready

logger = logging.getLogger(__name__)


def pulumi_stack_name(default: str = "polynote-ft") -> str:
    return os.environ.get("PULUMI_STACK", default)


@dataclass(frozen=True)
class PolynoteEnv:
    stack_name: str
    outputs: Dict[str, Any]


class PulumiPolynoteRunner:
    """
    Owns the lifecycle of a Pulumi stack for PolyNote functional tests.
    Pure Python (not pytest-specific), so it's easy to test and reuse.
    """

    def __init__(
        self,
        *,
        project_name: str,
        work_dir: str,
        stack_name: Optional[str] = None,
        node_ids: Iterable[str],
    ):
        self.project_name = project_name
        self.work_dir = work_dir

        if stack_name is None:
            stack_name = pulumi_stack_name()

        self.stack_name = stack_name
        self.node_ids = tuple(node_ids)
        self._stack: Optional[auto.Stack] = None

    def up(self) -> PolynoteEnv:
        logger.info("create/select stack: project=%s stack=%s", self.project_name, self.stack_name)

        self._stack = auto.create_or_select_stack(
            stack_name=self.stack_name,
            work_dir=self.work_dir,
            project_name=self.project_name,
        )

        self._stack.refresh(on_output=None, on_error=print)
        up_res = self._stack.up(on_output=None, on_error=print)

        outputs = {k: v.value for k, v in up_res.outputs.items()}
        self._validate_outputs(outputs)

        for node_id in self.node_ids:
            wait_http_ready(node_id, self.stack_name)
            logger.info("Node %s is reachable.", node_id)

        return PolynoteEnv(stack_name=self.stack_name, outputs=outputs)

    def destroy(self) -> None:
        if not self._stack:
            return
        logger.info("Destroying stack=%s ...", self.stack_name)
        self._stack.destroy(on_output=print)
        logger.info("Teardown complete.")

    def _validate_outputs(self, outputs: Dict[str, Any]) -> None:
        # Keep this strict so failures are obvious.
        deployment_names = outputs.get("deploymentNames")
        service_names = outputs.get("serviceNames")

        if deployment_names is None or service_names is None:
            raise AssertionError(
                f"Missing expected outputs. Got keys: {list(outputs.keys())}. "
                f"Expected 'deploymentNames' and 'serviceNames'."
            )

        for node_id in self.node_ids:
            if node_id not in deployment_names:
                raise AssertionError(f"Node {node_id} missing in outputs['deploymentNames']: {deployment_names}")
            if node_id not in service_names:
                raise AssertionError(f"Node {node_id} missing in outputs['serviceNames']: {service_names}")