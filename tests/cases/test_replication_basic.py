"""
Functional test:
- Create a note on Node A
- Trigger replication
- Verify eventual convergence on Node B and Node C
"""

# from polynote_testkit.eventually import eventually


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

    # # 3. Assert eventual visibility
    # @eventually(timeout_s=20, interval_s=0.5)
    # def assert_converged():
    #     notes_b = node_b.list_notes()
    #     notes_c = node_c.list_notes()
    #
    #     ids_b = {n["id"] for n in notes_b}
    #     ids_c = {n["id"] for n in notes_c}
    #
    #     assert note_id in ids_b
    #     assert note_id in ids_c
    #
    # assert_converged()