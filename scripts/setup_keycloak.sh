#!/bin/bash

KEYCLOAK_URL="http://localhost:10091"
ADMIN_USER="admin"
ADMIN_PASS="admin"

echo "Waiting for Keycloak..."
until curl -s $KEYCLOAK_URL > /dev/null; do
  sleep 5
  echo "Retrying..."
done

echo "Getting Admin Token..."
TOKEN=$(curl -s -d "client_id=admin-cli" -d "username=$ADMIN_USER" -d "password=$ADMIN_PASS" -d "grant_type=password" "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" | jq -r '.access_token')

if [ "$TOKEN" == "null" ]; then
  echo "Failed to get token"
  exit 1
fi

echo "Creating Realm 'taskforge'..."
curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"realm": "taskforge", "enabled": true}' \
  "$KEYCLOAK_URL/admin/realms"

echo "Creating Client 'taskforge-api'..."
curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"clientId": "taskforge-api", "enabled": true, "publicClient": true, "directAccessGrantsEnabled": true, "redirectUris": ["*"], "webOrigins": ["*"]}' \
  "$KEYCLOAK_URL/admin/realms/taskforge/clients"

echo "Creating User 'sahilvadia7776@gmail.com'..."
curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"username": "sahilvadia7776@gmail.com", "email": "sahilvadia7776@gmail.com", "enabled": true, "firstName": "Sahil", "lastName": "Vadia"}' \
  "$KEYCLOAK_URL/admin/realms/taskforge/users"

echo "Getting User ID..."
USER_ID=$(curl -s -H "Authorization: Bearer $TOKEN" "$KEYCLOAK_URL/admin/realms/taskforge/users?username=sahilvadia7776@gmail.com" | jq -r '.[0].id')

echo "Setting Password..."
curl -s -X PUT -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"type": "password", "value": "123", "temporary": false}' \
  "$KEYCLOAK_URL/admin/realms/taskforge/users/$USER_ID/reset-password"

echo "Done!"
