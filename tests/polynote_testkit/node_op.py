from tests.polynote_testkit.client import PolyNoteClient

from .assertions import assert_notes_equal


def create_note(node: PolyNoteClient) -> dict:
    return node.create_note(
        title="hello polynote",
        body=f"created on node {node.node_id}"
    )


def create_note_then_update(node: PolyNoteClient, update_count: int):
    created = node.create_note(
        title="hello polynote",
        body=f"created on node {node.node_id}"
    )

    for n in range(update_count):
        node.update_note(
            created["id"],
            n + 1,
            title="hello polynote - updated title",
            body=f"created on node {node.node_id} - updated body {n}",
        )


def create_note_then_update_then_delete(node: PolyNoteClient, update_count: int):
    created = node.create_note(
        title="hello polynote",
        body=f"created on node {node.node_id}"
    )
    note_id = created["id"]

    for n in range(update_count):
        node.update_note(
            note_id,
            n + 1,
            title="hello polynote - updated title",
            body=f"created on node {node.node_id} - updated body {n}",
        )

    node.delete_note(note_id, n + 2)


def sync_and_assert_notes_equal(node1: PolyNoteClient, node2: PolyNoteClient, expected_count: int):
    node1.sync_from(node2.node_id)
    node2.sync_from(node1.node_id)

    notes1 = node1.list_notes()
    notes2 = node2.list_notes()

    assert_notes_equal(notes1, notes2, expected_count=expected_count)
