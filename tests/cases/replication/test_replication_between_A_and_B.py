import pytest
from polynote_testkit.node_op import create_note_then_update_then_delete
from tests.polynote_testkit.node_op import create_note_then_update, sync_and_assert_notes_equal, create_note


def test_create_note_replicates_between_2_nodes(node_a, node_b):
    create_note(node_a)
    create_note(node_b)
    sync_and_assert_notes_equal(node_a, node_b, expected_count=2)


@pytest.mark.parametrize(
    "update_count_a,update_count_b",
    [(1, 2), (2, 1), (3, 5), (5, 4), (10, 8)],
)
def test_create_and_update_note_replicates_between_2_nodes(node_a, node_b, update_count_a, update_count_b):
    create_note_then_update(node_a, update_count_a)
    create_note_then_update(node_b, update_count_b)
    sync_and_assert_notes_equal(node_a, node_b, expected_count=2)


@pytest.mark.parametrize(
    "update_count_a,update_count_b",
    [(1, 2), (2, 1), (3, 5), (5, 4), (10, 8)],
)
def test_create_update_delete_note_replicates_between_2_nodes(node_a, node_b, update_count_a, update_count_b):
    create_note_then_update_then_delete(node_a, update_count_a)
    create_note_then_update_then_delete(node_b, update_count_b)
    sync_and_assert_notes_equal(node_a, node_b, expected_count=0)
