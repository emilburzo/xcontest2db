#!/usr/bin/env bash
set -euo pipefail

IMAGE="playwright-content"
CONTAINER="playwright-content"
PORT="${PORT:-3000}"
PROXY_SERVER="${PROXY_SERVER:-}"
PROXY_USERNAME="${PROXY_USERNAME:-}"
PROXY_PASSWORD="${PROXY_PASSWORD:-}"

case "${1:-help}" in
  build)
    echo "Building $IMAGE..."
    docker build -t "$IMAGE" "$(dirname "$0")"
    echo "Done."
    ;;

  start)
    if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
      echo "$CONTAINER is already running."
      exit 0
    fi
    docker rm -f "$CONTAINER" 2>/dev/null || true
    echo "Starting $CONTAINER on port $PORT..."
    docker run -d --name "$CONTAINER" -p "$PORT:3000" --restart unless-stopped \
      -e PROXY_SERVER="$PROXY_SERVER" \
      -e PROXY_USERNAME="$PROXY_USERNAME" \
      -e PROXY_PASSWORD="$PROXY_PASSWORD" \
      "$IMAGE"
    echo "Waiting for server..."
    for i in $(seq 1 15); do
      if curl -sf "http://localhost:$PORT/" >/dev/null 2>&1 || \
         docker logs "$CONTAINER" 2>&1 | grep -q "listening on port"; then
        echo "Ready at http://localhost:$PORT/content"
        exit 0
      fi
      sleep 1
    done
    echo "Server started (check 'docker logs $CONTAINER' if issues arise)."
    ;;

  stop)
    echo "Stopping $CONTAINER..."
    docker stop "$CONTAINER" 2>/dev/null && docker rm "$CONTAINER" 2>/dev/null || echo "Not running."
    ;;

  logs)
    docker logs -f "$CONTAINER"
    ;;

  test)
    echo "Testing base page (page 1)..."
    RESULT=$(curl -sX POST "http://localhost:$PORT/content" \
      -H 'Content-type: application/json' \
      -d '{"url":"https://www.xcontest.org/romania/zboruri/","waitFor":".XClist"}')
    ROWS=$(echo "$RESULT" | grep -c '<tr' || true)
    PILOT1=$(echo "$RESULT" | grep -oP 'detalii:\K[^"]+' | head -1 || true)
    if [ "$ROWS" -gt 50 ]; then
      echo "  OK - $ROWS rows, first pilot: $PILOT1"
    else
      echo "  FAIL - only $ROWS rows"
      exit 1
    fi

    echo "Testing hash page (offset 200)..."
    RESULT=$(curl -sX POST "http://localhost:$PORT/content" \
      -H 'Content-type: application/json' \
      -d '{"url":"https://www.xcontest.org/romania/zboruri/#flights[start]=200","waitFor":".XClist"}')
    ROWS=$(echo "$RESULT" | grep -c '<tr' || true)
    PILOT2=$(echo "$RESULT" | grep -oP 'detalii:\K[^"]+' | head -1 || true)
    if [ "$ROWS" -gt 50 ] && [ "$PILOT2" != "$PILOT1" ]; then
      echo "  OK - $ROWS rows, first pilot: $PILOT2 (different from page 1)"
    else
      echo "  FAIL - rows=$ROWS, pilot=$PILOT2 (expected >50 rows and different pilot from page 1: $PILOT1)"
      exit 1
    fi

    echo "All tests passed."
    ;;

  *)
    echo "Usage: $0 {build|start|stop|logs|test}"
    echo ""
    echo "  build   Build the Docker image"
    echo "  start   Start the container (PORT=$PORT)"
    echo "  stop    Stop and remove the container"
    echo "  logs    Tail container logs"
    echo "  test    Run a quick smoke test against a running instance"
    ;;
esac
