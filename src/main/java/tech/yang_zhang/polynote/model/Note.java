package tech.yang_zhang.polynote.model;

// todo: How should Note handles different ts types: Lamport (long) vs Vector ?
//  for now, just use String
public record Note(String id, String title, String body, String ts, long updatedAt, String updatedBy, boolean tomestoned) {
}
