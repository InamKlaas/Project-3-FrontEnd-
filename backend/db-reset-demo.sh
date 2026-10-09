#!/bin/sh
# Reset the LOCAL demo database. Never runs automatically, never touches prod.
# Usage: DB_URL='jdbc:mysql://localhost:3306/cput_home' ./backend/db-reset-demo.sh
# Asks twice. Refuses anything that does not look local.
set -e

DB_URL="${DB_URL:?set DB_URL to your local jdbc url, e.g. jdbc:mysql://localhost:3306/cput_home}"

case "$DB_URL" in
  *localhost*|*127.0.0.1*) ;;
  *) echo "refusing: '$DB_URL' is not localhost"; exit 1 ;;
esac

echo "This DROPS every table in the database behind:"
echo "  $DB_URL"
printf "Type the database name to continue: "
read CONFIRM
NAME=$(printf '%s' "$DB_URL" | sed -E 's#.*/([^?]+).*#\1#')
if [ "$CONFIRM" != "$NAME" ]; then
  echo "aborted."
  exit 1
fi

# shellcheck disable=SC2039
MYSQL_PWD="${DB_PASSWORD:?set DB_PASSWORD}" mysql -h 127.0.0.1 -u "${DB_USERNAME:-root}" -N -e "
SET FOREIGN_KEY_CHECKS = 0;
SET GROUP_CONCAT_MAX_LEN = 32768;
SELECT CONCAT('DROP TABLE IF EXISTS \`', table_name, '\`;')
FROM information_schema.tables
WHERE table_schema = DATABASE();" | MYSQL_PWD="${DB_PASSWORD:?set DB_PASSWORD}" mysql -h 127.0.0.1 -u "${DB_USERNAME:-root}" "$NAME"

echo "dropped. restart the backend (dev profile) to migrate + reseed."
