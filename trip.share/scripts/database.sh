#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
repo_dir="$(cd -- "$project_dir/.." && pwd)"

cd "$project_dir"

compose=(
    docker compose
    --project-directory "$repo_dir"
    -f "$repo_dir/docker-compose.yaml"
)

usage() {
    echo "Usage: $0 {start|restart|migrate|status} [--demo]" >&2
    exit 2
}

[[ $# -ge 1 && $# -le 2 ]] || usage
action="$1"
demo=false
if [[ $# -eq 2 ]]; then
    [[ "$2" == --demo && ( "$action" == start || "$action" == restart ) ]] || usage
    demo=true
fi
case "$action" in start|restart|migrate|status) ;; *) usage ;; esac

if "$demo"; then
    # Compose resolves .env itself; do not source arbitrary shell code from it.
    environment="$("${compose[@]}" config --environment | sed -n 's/^APP_ENV=//p')"
    [[ "${environment:-development}" == development ]] || { echo "Demo seeds require APP_ENV=development." >&2; exit 1; }
fi

case "$action" in
    start) "${compose[@]}" up -d --wait postgres ;;
    restart)
        "${compose[@]}" restart postgres
        "${compose[@]}" up -d --wait postgres
        ;;
esac

if [[ "$action" == status ]]; then
    "${compose[@]}" --profile tools run --build --rm --no-deps migrate flyway:info
else
    if ! "${compose[@]}" --profile tools run --build --rm --no-deps migrate; then
        echo "Migration failed. Data was preserved; inspect the error. No automatic baseline, repair or clean is performed." >&2
        exit 1
    fi
    if "$demo"; then
        "${compose[@]}" exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < "$project_dir/scripts/seeds/demo.sql"
    fi
fi
