📝 PolyNote

A hands-on project to learn Multi-Leader Replication, Conflict Resolution, and Local-First Architecture


⸻

📘 Overview

PolyNote is a distributed multi-leader replicated notes system inspired by concepts from
Designing Data-Intensive Applications (DDIA).

All replicas (Node A, B, C) function as independent leaders:
	•	Each node stores its own local database
	•	Accepts writes offline
	•	Replicates asynchronously
	•	Resolves conflicts
	•	Eventually converges

This project helps you learn the core principles behind real-world systems like:
	•	1Password
	•	Dropbox
	•	Notion’s offline mode
	•	Git
	•	CouchDB

⸻

🎯 Learning Objectives

By completing PolyNote, you will deeply understand:
	•	Multi-leader replication
	•	Replication logs
	•	Async propagation
	•	Eventual consistency
	•	Conflict detection
	•	Conflict resolution strategies (LWW / CRDT / OT)
	•	Local-first application design
	•	Convergence across distributed replicas

⸻

🚀 Features to Implement

Below is the complete specification you’ll build.

⸻

1. 📦 Local Data Store

Each node maintains its own independent local storage (SQLite).

Notes must follow this schema:

{
  "id": "uuid",
  "title": "string",
  "body": "string",
  "updated_at": "timestamp",
  "updated_by": "node-id"
}


⸻

2. 📝 CRUD API (Writable Everywhere)

Every node exposes:

```
POST /notes

GET /notes

PUT /notes/{id}

DELETE /notes/{id}
```

All nodes are writable, simulating multi-leader behavior.

Each write increments a local timestamp or logical counter.

⸻

3. 📜 Replication Log

Every write generates a durable log entry:

{
  "seq": "monotonically increasing integer",
  "op_id": "uuid",
  "ts": "logical timestamp or wall clock",
  "node_id": "A|B|C", 
  "type": "CREATE|UPDATE|DELETE",
  "note_id": "uuid",
  "payload": {}
}

Nodes expose:

GET /replication/log?since=<seq>


⸻

4. 🔄 Asynchronous Replication

Every node:
	•	Periodically (e.g., every 5 seconds) fetches others’ logs
	•	Applies remote operations
	•	Appends remote operations to its own log

This simulates asynchronous multi-leader replication.

⸻

5. ⚠️ Conflict Handling (Critical Requirement)

Implement the two major conflict types from DDIA:

🛑 Conflict 1: Concurrent Updates

Two nodes update the same note before syncing.

🗑️ Conflict 2: Delete vs Update

One node deletes a note while another updates it.

⸻

6. 🔧 Conflict Resolution

Implement at least one strategy (more = better):

Option A: Last Writer Wins (LWW)

Deterministic ordering using (timestamp, node_id).

Option B: Field-Level Merge

Merge title/body independently.

Option C: Operational Transform (OT)

Text-editor style transformation.

Option D: CRDT: LWW-Register

Use CRDT logic to ensure convergence.

Option E: Surface both versions to user

Store:

<<<< LOCAL VERSION
...
====
<<<< REMOTE VERSION
...

⸻

10. 🎖️ Bonus Features (Advanced)

If you want to go deeper:

A. Vector Clocks

Detect concurrent vs causal updates.

B. Causal Delivery

Delay applying operations until dependencies arrive.

C. Full Snapshot Export

GET /snapshot

D. Anti-Entropy Repair Job

Background repair to detect and fix divergence.

⸻

🧪 Test Plan

A complete test suite should include:
	•	✔ Create notes on Node A → replicate to B/C
	•	✔ Update same note concurrently on A/B → conflict → resolve
	•	✔ Delete on A while updating on B → resolve
	•	✔ Final consistency: all nodes share identical state

⸻

🏗️ Tech Stack

Java
	•	Spring Boot
	•	SQLite

⸻
