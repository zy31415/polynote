#!/usr/bin/env bash

# ============================================================
# PolyNote Port Forward Manager
#
# Usage:
#   ./port-forward.sh start     Start port-forwarding for A/B/C
#   ./port-forward.sh clean     Kill all running port-forwards
#   ./port-forward.sh help      Show detailed help
#
# Ports:
#   A → localhost:8081 → polynote-a:8080
#   B → localhost:8082 → polynote-b:8080
#   C → localhost:8083 → polynote-c:8080
#
# Logs:
#   port-a.log
#   port-b.log
#   port-c.log
#
# This script uses `ps aux | grep "kubectl port-forward"` to detect
# running processes and clean them up—no PID files needed.
# ============================================================

# Local → Remote ports
PORT_A_LOCAL=8081
PORT_B_LOCAL=8082
PORT_C_LOCAL=8083
PORT_REMOTE=8080

DEPLOY_A="polynote-a"
DEPLOY_B="polynote-b"
DEPLOY_C="polynote-c"

show_help() {
    cat << EOF
PolyNote Port Forward Manager

Usage:
  $0 start       Start port-forwarding the 3 PolyNote deployments
  $0 clean       Kill all running kubectl port-forward processes
  $0 help        Show this help message

Description:
  This script forwards local ports to the three PolyNote nodes:

      localhost:8081  →  deployment/polynote-a:8080
      localhost:8082  →  deployment/polynote-b:8080
      localhost:8083  →  deployment/polynote-c:8080

  Each forward runs in the background using 'nohup'. Logs are written to:

      port-a.log
      port-b.log
      port-c.log

  The 'clean' command detects all port-forward processes using:

      ps aux | grep "kubectl port-forward"

  And terminates them gracefully.

Examples:
  Start all port-forwards:
      $0 start

  Stop all port-forwards:
      $0 clean

  Display help:
      $0 help
EOF
}

start_port_forward() {
    echo "Starting PolyNote port forwarding..."

    nohup kubectl port-forward deployment/$DEPLOY_A $PORT_A_LOCAL:$PORT_REMOTE > logs/port-a.log 2>&1 &
    echo "Forward A: localhost:$PORT_A_LOCAL → $DEPLOY_A:$PORT_REMOTE"

    nohup kubectl port-forward deployment/$DEPLOY_B $PORT_B_LOCAL:$PORT_REMOTE > logs/port-b.log 2>&1 &
    echo "Forward B: localhost:$PORT_B_LOCAL → $DEPLOY_B:$PORT_REMOTE"

    nohup kubectl port-forward deployment/$DEPLOY_C $PORT_C_LOCAL:$PORT_REMOTE > logs/port-c.log 2>&1 &
    echo "Forward C: localhost:$PORT_C_LOCAL → $DEPLOY_C:$PORT_REMOTE"

    echo "All forwards started. Logs available in logs/port-*.log"
}

clean_port_forward() {
    echo "Cleaning all kubectl port-forward processes..."

    PIDS=$(ps aux | grep "kubectl port-forward" | grep -v grep | awk '{print $2}')

    if [ -z "$PIDS" ]; then
        echo "No port-forward processes found."
        exit 0
    fi

    echo "Found processes: $PIDS"
    for PID in $PIDS; do
        echo "Killing PID $PID"
        kill "$PID"
    done

    echo "Cleanup complete."
}

case "$1" in
    start)
        start_port_forward
        ;;
    clean)
        clean_port_forward
        ;;
    help|-h|--help)
        show_help
        ;;
    *)
        echo "Unknown command: $1"
        echo "Run '$0 help' for usage."
        exit 1
        ;;
esac