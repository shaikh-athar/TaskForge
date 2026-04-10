#!/bin/bash

# Configuration
URL="http://localhost:8081/api/v1/auth/login"
LIMIT=25 # Limit is 20, so 25 should trigger 429
DELAY=0.1

echo "Starting rate limit verification..."
echo "Target: $URL"
echo "Sending $LIMIT requests..."

for ((i=1; i<=LIMIT; i++)); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$URL")
    
    if [ "$STATUS" -eq 429 ]; then
        echo "Request $i: $STATUS (Too Many Requests) - RATE LIMITING WORKING!"
        exit 0
    else
        echo "Request $i: $STATUS"
    fi
    
    sleep $DELAY
done

echo "Finished sending requests. failed to trigger 429."
echo "Note: Ensure the backend is running and the rate limit count hasn't been reset/expired."
exit 1
