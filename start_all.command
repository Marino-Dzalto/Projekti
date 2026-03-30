#!/bin/zsh
set -e

cd "$(dirname "$0")"

# PATH za Automator
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

# 1) Spring server
cd SwissOnlineServer
./mvnw -q spring-boot:run > ../server.log 2>&1 &
SERVER_PID=$!
cd ..

# čekaj da server odgovara (0.2s intervali umjesto 1s)
for i in {1..150}; do
  if curl -s http://localhost:8080/ >/dev/null 2>&1; then
    break
  fi
  sleep 0.2
done

# 2) ngrok - pokreni u pozadini, ne čekaj
/opt/homebrew/bin/ngrok http 8080 > ngrok.log 2>&1 &
NGROK_PID=$!

# 3) Desktop app - kompajliraj samo ako je .java noviji od .class
if [ SwissTournamentApp.java -nt SwissTournamentApp.class ]; then
  echo "Kompajliram..."
  javac SwissTournamentApp.java
fi
java SwissTournamentApp

# cleanup
kill $NGROK_PID >/dev/null 2>&1 || true
kill $SERVER_PID >/dev/null 2>&1 || true
