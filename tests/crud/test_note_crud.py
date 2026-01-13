import pytest

from tests.polynote_testkit.node_op import create_note


@pytest.mark.parametrize("create_count", [1, 2, 3, 5, 10])
def test_create_note(node_a, create_count):
    for i in range(create_count):
        create_note(node_a, f"Test Note {i}", f"This is a test note {i}")
    notes = node_a.list_notes()

    assert len(notes) == create_count

    for i, note in enumerate(notes):
        assert note["title"] == f"Test Note {i}"
        assert note["body"] == f"This is a test note {i}"
        assert note["ts"] == f'{i+1}'


@pytest.mark.parametrize("update_count", [1, 2, 3, 5, 10])
def test_update_note(node_a, update_count):
    created = create_note(node_a, "Test Note for update", "This is a test note for update")

    n = 0
    for n in range(update_count):
        node_a.update_note(
            created["id"],
            n + 1,
            title=f"hello polynote - updated title {n}",
            body=f"created on node {node_a.node_id} - updated body {n}",
            )

    notes = node_a.list_notes()
    assert len(notes) == 1

    note = notes[0]

    assert note["title"] == f"hello polynote - updated title {n}"
    assert note["body"] == f"created on node {node_a.node_id} - updated body {n}"
    assert note["ts"] == f'{n+2}'


def test_create_note_then_delete(node_a):
    created = create_note(node_a, "Test Note for delete", "This is a test note for delete")
    node_a.delete_note(created["id"], ts=1)

    notes = node_a.list_notes()
    assert len(notes) == 0


def test_create_note_then_update_then_delete(node_a):
    created = create_note(node_a, "Test Note for update and delete", "This is a test note for update and delete")

    node_a.update_note(
        created["id"],
        ts=1,
        title="Updated title",
        body="Updated body"
    )

    node_a.delete_note(created["id"], ts=2)

    notes = node_a.list_notes()
    assert len(notes) == 0
