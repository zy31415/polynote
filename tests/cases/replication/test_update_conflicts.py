from tests.polynote_testkit.node_op import create_a_note, sync_and_assert_notes_equal


def test_update_conflicts_b_has_latest_update(node_a, node_b):
    created = create_a_note(node_a)
    node_b.sync_from(node_a.node_id)

    note_id = created["id"]

    node_a.update_note(note_id, ts=1, title="Update from A", body="This is an update from node A")
    node_b.update_note(note_id, ts=1, title="Update from B", body="This is an update from node B")

    sync_and_assert_notes_equal(node_a, node_b, 1)

    # Verify that LWW is node_b's update since node B has the higher Lamport timestamp
    note = node_a.list_notes()[0]
    assert note["title"] == "Update from B"
    assert note["body"] == "This is an update from node B"
    assert note["updatedAt"] == 3
    assert note["updatedBy"] == "B"


def test_update_conflicts_tie_breaker(node_a, node_b):
    created = create_a_note(node_a)
    node_b.sync_from(node_a.node_id)

    note_id = created["id"]

    node_a.update_note(note_id, ts=1, title="Update from A", body="This is 1st update from node A")
    node_a.update_note(note_id, ts=2, title="Update from A", body="This is 2st update from node A")

    node_b.update_note(note_id, ts=1, title="Update from B", body="This is an update from node B")

    sync_and_assert_notes_equal(node_a, node_b, 1)

    # Verify that LWW is node_b's update since node B has the higher Lamport timestamp
    note = node_a.list_notes()[0]
    assert note["title"] == "Update from A"
    assert note["body"] == "This is 2st update from node A"
    assert note["updatedAt"] == 3
    assert note["updatedBy"] == "A"


def test_update_conflicts_a_has_latest_update(node_a, node_b):
    created = create_a_note(node_a)
    node_b.sync_from(node_a.node_id)

    note_id = created["id"]

    node_a.update_note(note_id, ts=1, title="Update from A", body="This is 1st update from node A")
    node_a.update_note(note_id, ts=2, title="Update from A", body="This is 2st update from node A")
    node_a.update_note(note_id, ts=3, title="Update from A", body="This is 3rd update from node A")

    node_b.update_note(note_id, ts=1, title="Update from B", body="This is an update from node B")

    sync_and_assert_notes_equal(node_a, node_b, 1)

    # Verify that LWW is node_b's update since node B has the higher Lamport timestamp
    note = node_a.list_notes()[0]
    assert note["title"] == "Update from A"
    assert note["body"] == "This is 3rd update from node A"
    assert note["updatedAt"] == 4
    assert note["updatedBy"] == "A"