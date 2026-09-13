#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="p67906265-spec/allenamento-portieri"
ALIAS="allenamento_portieri"
FIRMA_DIR="$HOME/firme-android/allenamento-portieri"
KEYSTORE="$FIRMA_DIR/allenamento-portieri-release.jks"
BACKUP_DIR="/sdcard/Download/Firma_Allenamento_Portieri"

printf '\nFirma permanente - Allenamento Portieri\n\n'
mkdir -p "$FIRMA_DIR"

if ! command -v openssl >/dev/null 2>&1; then
  echo "Installo OpenSSL..."
  pkg install -y openssl
fi
if ! command -v gh >/dev/null 2>&1; then
  echo "Installo GitHub CLI..."
  pkg install -y gh
fi

if [ -s "$KEYSTORE" ]; then
  echo "La firma esiste già e non verrà sostituita:"
  echo "$KEYSTORE"
  printf "Password della firma esistente: "
  read -rs STORE_PASSWORD
  printf '\n'
else
  while true; do
    printf "Scegli la password della firma (almeno 8 caratteri): "
    read -rs STORE_PASSWORD
    printf '\nRipeti la password: '
    read -rs CONFIRM_PASSWORD
    printf '\n'
    [ ${#STORE_PASSWORD} -ge 8 ] || { echo "Password troppo corta."; continue; }
    [ "$STORE_PASSWORD" = "$CONFIRM_PASSWORD" ] && break
    echo "Le password non coincidono. Riprova."
  done
  TEMP_KEY="$FIRMA_DIR/chiave-temporanea.pem"
  TEMP_CERT="$FIRMA_DIR/certificato-temporaneo.pem"
  trap 'rm -f "$TEMP_KEY" "$TEMP_CERT"' EXIT
  rm -f "$KEYSTORE"
  openssl req -x509 -newkey rsa:4096 -sha256 -days 10000 -nodes \
    -keyout "$TEMP_KEY" -out "$TEMP_CERT" \
    -subj "/C=IT/L=Italia/O=Paolo Free/OU=Android/CN=Paolo Free"
  openssl pkcs12 -export -name "$ALIAS" -inkey "$TEMP_KEY" -in "$TEMP_CERT" \
    -out "$KEYSTORE" -passout "pass:$STORE_PASSWORD"
  rm -f "$TEMP_KEY" "$TEMP_CERT"
fi

openssl pkcs12 -in "$KEYSTORE" -passin "pass:$STORE_PASSWORD" -nokeys -noout >/dev/null

if ! gh auth status >/dev/null 2>&1; then
  echo "Ora accedi a GitHub. Scegli GitHub.com e HTTPS."
  gh auth login
fi

echo "Carico automaticamente i quattro segreti nel repository..."
base64 -w 0 "$KEYSTORE" | gh secret set SIGNING_KEY --repo "$REPO"
printf '%s' "$STORE_PASSWORD" | gh secret set SIGNING_STORE_PASSWORD --repo "$REPO"
printf '%s' "$ALIAS" | gh secret set SIGNING_KEY_ALIAS --repo "$REPO"
printf '%s' "$STORE_PASSWORD" | gh secret set SIGNING_KEY_PASSWORD --repo "$REPO"

mkdir -p "$BACKUP_DIR"
cp -f "$KEYSTORE" "$BACKUP_DIR/allenamento-portieri-release.jks"
chmod 600 "$KEYSTORE"

unset STORE_PASSWORD CONFIRM_PASSWORD
printf '\nFatto. I quattro segreti sono configurati.\n'
echo "Copia della firma salvata in: $BACKUP_DIR"
echo "Conserva il file e la password in un posto sicuro."
