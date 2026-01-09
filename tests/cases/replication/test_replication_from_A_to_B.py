import pytest

from tests.polynote_testkit.assertions import assert_notes_equal
from tests.polynote_testkit.node_op import create_note_then_update, create_note_then_update_then_delete, create_note


def test_create_note_replicates_to_another_node(node_a, node_b):
    create_note(node_a)
    node_b.sync_from(node_a.node_id)
    assert_notes_equal(node_a.list_notes(), node_b.list_notes(), expected_count=1)


@pytest.mark.parametrize("update_count", [1, 2, 3, 5, 10])
def test_create_update_note_replicates_to_another_node(node_a, node_b, update_count):
    create_note_then_update(node_a, update_count)
    node_b.sync_from("A")
    assert_notes_equal(node_a.list_notes(), node_b.list_notes(), expected_count=1)


@pytest.mark.parametrize("update_count", [1, 2, 3, 5, 10])
def test_create_update_delete_note_replicates_to_another_node(node_a, node_b, update_count):
    create_note_then_update_then_delete(node_a, update_count)
    node_b.sync_from("A")
    assert_notes_equal(node_a.list_notes(), node_b.list_notes(), expected_count=0)
