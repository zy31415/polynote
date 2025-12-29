import pytest


@pytest.fixture(scope="session")
def node_client_factory(polynote_env):
    stack_name = polynote_env["stack_name"]

    def make(node_id: str):
        return PolyNoteClient(node_id=node_id, stack_name=stack_name)

    return make


@pytest.fixture(scope="session")
def nodes(node_client_factory, node_ids):
    """
    Returns a dict: { "a": PolyNoteClient, ... }
    """
    return {node_id: node_client_factory(node_id) for node_id in node_ids}


@pytest.fixture(scope="session")
def node_a(nodes):
    return nodes["a"]

@pytest.fixture(scope="session")
def node_b(nodes):
    return nodes["b"]

@pytest.fixture(scope="session")
def node_c(nodes):
    return nodes["c"]