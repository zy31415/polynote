"""
Functional test:
- Create a note on Node A
- Trigger replication
- Verify eventual convergence on Node B and Node C
"""
def test_create_note_replicates_to_all_nodes(node_a, node_b, node_c):
    # 1. Create note on Node A
    created = node_a.create_note(
        title="hello polynote",
        body="created on node A"
    )
    note_id = created["id"]

    # 2. Trigger replication pulls
    # (explicit is better than implicit for tests)
    node_b.sync_from("A")
    node_c.sync_from("A")

    notes_b = node_b.list_notes()
    notes_c = node_c.list_notes()

    assert len(notes_b) == 1 and created == notes_b[0]
    assert len(notes_c) == 1 and created == notes_c[0]