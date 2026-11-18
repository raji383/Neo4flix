#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/docs/logs"
PID_DIR="$LOG_DIR/.pids"
BACKEND_DIR="$ROOT_DIR/backend"
SERVICES=(registry user-service movie-service rating-service recommendation-service api-gateway)

usage() {
  cat <<'EOF'
Usage:
  ./run-services.sh start <service|all>
  ./run-services.sh stop <service|all>
  ./run-services.sh status [service|all]

Services: registry, user-service (user), movie-service (movie),
          rating-service, recommendation-service, api-gateway
This script does not start Neo4j or the frontend.
EOF
}

normalize_service() {
  case "$1" in
    user) printf '%s\n' user-service ;;
    movie) printf '%s\n' movie-service ;;
    *) printf '%s\n' "$1" ;;
  esac
}

is_known_service() {
  local candidate
  for candidate in "${SERVICES[@]}"; do
    [[ "$candidate" == "$1" ]] && return 0
  done
  return 1
}

pid_file() {
  printf '%s/%s.pid\n' "$PID_DIR" "$1"
}

service_process_matches() {
  local service="$1"
  local pid="$2"
  local module_dir="$BACKEND_DIR/$service"
  local process_dir process_args process_group

  [[ "$pid" =~ ^[0-9]+$ ]] || return 1
  kill -0 "$pid" 2>/dev/null || return 1
  [[ -r "/proc/$pid/cmdline" ]] || return 1
  process_dir="$(readlink -f "/proc/$pid/cwd" 2>/dev/null)" || return 1
  [[ "$process_dir" == "$module_dir" ]] || return 1
  process_args="$(tr '\0' ' ' < "/proc/$pid/cmdline")"
  [[ "$process_args" == *"spring-boot:run"* ]] || return 1
  process_group="$(ps -o pgid= -p "$pid" | tr -d ' ')"
  [[ "$process_group" == "$pid" ]]
}

start_service() {
  local service="$1"
  local module_dir="$BACKEND_DIR/$service"
  local log_file="$LOG_DIR/$service.log"
  local pid_path pid

  if [[ ! -x "$module_dir/mvnw" ]]; then
    echo "Missing executable Maven wrapper: $module_dir/mvnw" >&2
    return 1
  fi
  if ! command -v setsid >/dev/null 2>&1; then
    echo "The setsid command is required to manage backend process groups." >&2
    return 1
  fi

  pid_path="$(pid_file "$service")"
  if [[ -f "$pid_path" ]]; then
    read -r pid < "$pid_path"
    if service_process_matches "$service" "$pid"; then
      echo "$service is already running (PID $pid)."
      return 0
    fi
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
      echo "Refusing to replace stale $service PID file: PID $pid is not this service." >&2
      return 1
    fi
    rm -f -- "$pid_path"
  fi

  (
    cd "$module_dir"
    exec setsid ./mvnw spring-boot:run
  ) >> "$log_file" 2>&1 < /dev/null &
  pid=$!
  printf '%s\n' "$pid" > "$pid_path"
  sleep 1
  if ! kill -0 "$pid" 2>/dev/null; then
    rm -f -- "$pid_path"
    echo "$service exited during startup; check $log_file." >&2
    return 1
  fi
  echo "Started $service (PID $pid); logs: $log_file"
}

stop_service() {
  local service="$1"
  local pid_path pid

  pid_path="$(pid_file "$service")"
  if [[ ! -f "$pid_path" ]]; then
    echo "$service is not running."
    return 0
  fi

  read -r pid < "$pid_path"
  if ! [[ "$pid" =~ ^[0-9]+$ ]]; then
    echo "Invalid PID in $pid_path." >&2
    return 1
  fi
  if ! service_process_matches "$service" "$pid"; then
    if kill -0 "$pid" 2>/dev/null; then
      echo "Refusing to stop PID $pid: it does not match $service." >&2
      return 1
    fi
    rm -f -- "$pid_path"
    echo "Removed stale PID file for $service."
    return 0
  fi

  kill -TERM -- "-$pid"
  for _ in {1..10}; do
    kill -0 -- "-$pid" 2>/dev/null || break
    sleep 1
  done
  if kill -0 -- "-$pid" 2>/dev/null; then
    echo "$service did not stop after SIGTERM; sending SIGKILL." >&2
    kill -KILL -- "-$pid"
  fi
  rm -f -- "$pid_path"
  echo "Stopped $service."
}

show_status() {
  local service="$1"
  local pid_path pid

  pid_path="$(pid_file "$service")"
  if [[ ! -f "$pid_path" ]]; then
    echo "$service: stopped"
    return
  fi

  read -r pid < "$pid_path"
  if service_process_matches "$service" "$pid"; then
    echo "$service: running (PID $pid)"
  else
    echo "$service: not running (stale PID file)"
  fi
}

run_for_target() {
  local action="$1"
  local target="$2"
  local service
  local result=0

  if [[ "$target" == "all" ]]; then
    if [[ "$action" == "stop" ]]; then
      for ((index=${#SERVICES[@]}-1; index>=0; index--)); do
        service="${SERVICES[index]}"
        stop_service "$service" || result=1
      done
    else
      for service in "${SERVICES[@]}"; do
        if [[ "$action" == "start" ]]; then
          start_service "$service" || result=1
        else
          show_status "$service"
        fi
      done
    fi
    return "$result"
  fi

  service="$(normalize_service "$target")"
  if ! is_known_service "$service"; then
    echo "Unknown backend service: $target" >&2
    usage >&2
    return 2
  fi

  case "$action" in
    start) start_service "$service" ;;
    stop) stop_service "$service" ;;
    status) show_status "$service" ;;
  esac
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
  usage >&2
  exit 2
fi

action="$1"
target="${2:-all}"
if [[ "$action" != "start" && "$action" != "stop" && "$action" != "status" ]]; then
  usage >&2
  exit 2
fi
if [[ "$action" != "status" && $# -ne 2 ]]; then
  usage >&2
  exit 2
fi

mkdir -p "$LOG_DIR"
for service in "${SERVICES[@]}"; do
  touch "$LOG_DIR/$service.log"
done
if [[ "$action" != "status" || "$target" == "all" ]]; then
  mkdir -p "$PID_DIR"
fi

run_for_target "$action" "$target"
