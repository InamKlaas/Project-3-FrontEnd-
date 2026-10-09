#!/bin/sh
# Reset the LOCAL demo database. Never runs automatically, never touches prod.
# Usage: DB_URL='jdbc:mysql://localhost:3306/cput_home' ./backend/db-reset-demo.sh
# Asks twice. Refuses anything that does not look local.
set -e

DB_URL="${DB_URL:?set DB_URL to your local jdbc url, e.g. jdbc:mysql://localhost:3306/cput_home}"

case "$DB_URL" in jdbc:mysql://*) ;; *) echo "refusing: expected a MySQL JDBC URL"; exit 1 ;; esac
TARGET=${DB_URL#jdbc:mysql://}
HOSTPORT=${TARGET%%/*}
case "$HOSTPORT" in localhost|localhost:*|127.0.0.1|127.0.0.1:*) ;; *) echo "refusing: host is not loopback"; exit 1 ;; esac
PORT=3306
case "$HOSTPORT" in *:*) PORT=${HOSTPORT##*:} ;; esac
case "$PORT" in ''|*[!0-9]*) echo "invalid port"; exit 1 ;; esac
NAME=${TARGET#*/}
NAME=${NAME%%\?*}
case "$NAME" in ''|*[!A-Za-z0-9_]*) echo "invalid database name"; exit 1 ;; esac

echo "This DROPS every table in the database behind:"
echo "  $DB_URL"
printf "Type the database name to continue: "
read -r CONFIRM
if [ "$CONFIRM" != "$NAME" ]; then
  echo "aborted."
  exit 1
fi
printf "Type 'RESET %s' to confirm deletion: " "$NAME"
read -r CONFIRM
if [ "$CONFIRM" != "RESET $NAME" ]; then echo "aborted."; exit 1; fi

MYSQL_PWD="${DB_PASSWORD:?set DB_PASSWORD}" mysql -h 127.0.0.1 -P "$PORT" -u "${DB_USERNAME:-root}" -e "DROP DATABASE \`$NAME\`; CREATE DATABASE \`$NAME\`;"

echo "dropped. restart the backend (dev profile) to migrate + reseed."
