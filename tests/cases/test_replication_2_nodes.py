"""
Functional test:
- Create a note on Node A
- Trigger replication
- Verify eventual convergence on Node B and Node C
"""
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


def test_create_update_note_replicates_to_another_node(node_a, node_b):
    # 1. Create note on Node A
    created = node_a.create_note(
        title="hello polynote",
        body="created on node A"
    )
    note_id = created["id"]
    node_a.update_note(note_id, 1, title="updated title", body="updated body")

    # 2. Trigger replication pulls
    node_b.sync_from("A")

    notes_b = node_b.list_notes()

    assert len(notes_b) == 1 and created == notes_b[0]


def test_create_update_delete_note_replicates_to_another_node(node_a, node_b):
    # 1. Create note on Node A
    created = node_a.create_note(
        title="hello polynote",
        body="created on node A"
    )
    note_id = created["id"]
    node_a.update_note(note_id, 1, title="updated title", body="updated body")
    node_a.delete_note(note_id, 2)

    # 2. Trigger replication pulls
    node_b.sync_from("A")

    notes_a = node_a.list_notes()
    assert len(notes_a) == 0

    notes_b = node_b.list_notes()
    assert len(notes_b) == 0
