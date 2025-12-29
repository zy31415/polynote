import pytest

from tests.polynote_testkit.assertions import assert_notes_equal


def test_create_note_replicates_to_another_node(node_a, node_b):
    # 1. Create note on Node A
    created = node_a.create_note(
        title="hello polynote",
        body="created on node A"
    )
    note_id = created["id"]

    # 2. Trigger replication pulls
    # (explicit is better than implicit for tests)
    node_b.sync_from("A")

    notes_b = node_b.list_notes()

    assert len(notes_b) == 1 and created == notes_b[0]


@pytest.mark.parametrize("update_count", [1, 2, 3, 5, 10])
def test_create_update_note_replicates_to_another_node(node_a, node_b, update_count):
    created = node_a.create_note(title="hello polynote", body="created on node A")
    note_id = created["id"]

    for n in range(update_count):
        node_a.update_note(
            note_id,
            n + 1,
            title="hello polynote - updated title",
            body=f"created on node A - updated body {n}",
        )

    node_b.sync_from("A")
    assert_notes_equal(node_a.list_notes(), node_b.list_notes(), expected_count=1)


@pytest.mark.parametrize("update_count", [1, 2, 3, 5, 10])
def test_create_update_delete_note_replicates_to_another_node(node_a, node_b, update_count):
    # 1. Create note on Node A
    created = node_a.create_note(
        title="hello polynote",
        body="created on node A"
    )
    note_id = created["id"]

    for n in range(update_count):
        node_a.update_note(
            note_id,
            n + 1,
            title="hello polynote - updated title",
            body=f"created on node A - updated body {n}",
        )

    node_a.delete_note(note_id, n + 2)

    # 2. Trigger replication pulls
    node_b.sync_from("A")

    assert_notes_equal(node_a.list_notes(), node_b.list_notes(), expected_count=0)
