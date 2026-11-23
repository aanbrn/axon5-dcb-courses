#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Creates one login role and one database per service on the shared PostgreSQL server (D-24). The projections and the
# Axon token store of a service live in that service's database, so a service owns everything it writes.
#
# The PostgreSQL image runs this script only when the data directory is empty, that is once per volume. Passwords reach
# psql as variables and are interpolated as quoted literals (`:'name'`), never spliced into the SQL text.
set -euo pipefail

: "${QUERY_SERVICE_DB_PASSWORD:?QUERY_SERVICE_DB_PASSWORD is not set}"
: "${AUTOMATION_SERVICE_DB_PASSWORD:?AUTOMATION_SERVICE_DB_PASSWORD is not set}"

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v query_service_password="$QUERY_SERVICE_DB_PASSWORD" \
  -v automation_service_password="$AUTOMATION_SERVICE_DB_PASSWORD" <<'SQL'
CREATE ROLE query_service LOGIN PASSWORD :'query_service_password';
CREATE DATABASE query_service OWNER query_service;
CREATE ROLE automation_service LOGIN PASSWORD :'automation_service_password';
CREATE DATABASE automation_service OWNER automation_service;
SQL
