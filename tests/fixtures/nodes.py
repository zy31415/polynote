import pytest

@pytest.fixture(scope="session")
def node_ids() -> tuple[str, ...]:
    """
    Canonical set of PolyNote node IDs for this test run.
    """
    return ("a", "b", "c")
