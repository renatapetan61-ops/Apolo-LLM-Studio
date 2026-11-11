#!/usr/bin/env bash
set -euo pipefail

host_arch="$(uname -m)"
docker_arch="$(docker info --format '{{.Architecture}}')"

if [[ "$host_arch" != "x86_64" || "$docker_arch" != "x86_64" ]]; then
  echo "ERROR: Apolo executor requires a native x86_64 Linux Docker host."
  echo "Host kernel architecture: $host_arch"
  echo "Docker server architecture: $docker_arch"
  echo "Do not use QEMU/binfmt emulation: it is not native x86_64 CPU execution."
  exit 1
fi

docker version >/dev/null
docker compose version >/dev/null
echo "Native x86_64 host confirmed."
