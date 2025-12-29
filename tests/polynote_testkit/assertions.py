def assert_notes_equal(notes1, notes2, expected_count = None):
    if expected_count is not None:
        assert len(notes1) == expected_count
        assert len(notes2) == expected_count
    else:
        assert len(notes1) == len(notes2)

    assert {note["id"]:note for note in notes1} == {note["id"]:note for note in notes2}
