#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════════════════
#  BookPlus — RSA Key Generator (RS256 / JWT)
#
#  Usage:
#    chmod +x scripts/generate-keys.sh
#    ./scripts/generate-keys.sh
#
#  Output:
#    - Appends BOOKPLUS_JWT_PRIVATE_KEY_BASE64 and BOOKPLUS_JWT_PUBLIC_KEY_BASE64
#      to a .env file in the project root.
#    - Also prints the values to stdout for copy-paste into CI secrets.
# ════════════════════════════════════════════════════════════════════════════

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_ROOT/.env"
KEY_DIR="$PROJECT_ROOT/keys"

mkdir -p "$KEY_DIR"

echo "🔑  Generating RSA 2048-bit key pair..."

# Generate private key (PKCS#8 format required by Java KeyFactory)
openssl genrsa -out "$KEY_DIR/private.pem" 2048 2>/dev/null
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
    -in "$KEY_DIR/private.pem" -out "$KEY_DIR/private_pkcs8.pem"

# Extract public key (X.509 / SubjectPublicKeyInfo format)
openssl rsa -in "$KEY_DIR/private.pem" -pubout -out "$KEY_DIR/public.pem" 2>/dev/null

# Base64-encode (strip headers and newlines)
PRIVATE_B64=$(grep -v "BEGIN\|END" "$KEY_DIR/private_pkcs8.pem" | tr -d '\n')
PUBLIC_B64=$(grep -v "BEGIN\|END" "$KEY_DIR/public.pem" | tr -d '\n')

echo ""
echo "✅  Keys generated successfully."
echo ""
echo "BOOKPLUS_JWT_PRIVATE_KEY_BASE64=$PRIVATE_B64"
echo "BOOKPLUS_JWT_PUBLIC_KEY_BASE64=$PUBLIC_B64"
echo ""

# Write to .env (create from .env.example if missing)
if [ ! -f "$ENV_FILE" ]; then
    cp "$PROJECT_ROOT/.env.example" "$ENV_FILE"
    echo "📄  Created .env from .env.example"
fi

# Replace placeholder values in .env
if grep -q "^BOOKPLUS_JWT_PRIVATE_KEY_BASE64=" "$ENV_FILE"; then
    sed -i.bak "s|^BOOKPLUS_JWT_PRIVATE_KEY_BASE64=.*|BOOKPLUS_JWT_PRIVATE_KEY_BASE64=$PRIVATE_B64|" "$ENV_FILE"
else
    echo "BOOKPLUS_JWT_PRIVATE_KEY_BASE64=$PRIVATE_B64" >> "$ENV_FILE"
fi

if grep -q "^BOOKPLUS_JWT_PUBLIC_KEY_BASE64=" "$ENV_FILE"; then
    sed -i.bak "s|^BOOKPLUS_JWT_PUBLIC_KEY_BASE64=.*|BOOKPLUS_JWT_PUBLIC_KEY_BASE64=$PUBLIC_B64|" "$ENV_FILE"
else
    echo "BOOKPLUS_JWT_PUBLIC_KEY_BASE64=$PUBLIC_B64" >> "$ENV_FILE"
fi

rm -f "$ENV_FILE.bak"
echo "💾  Keys written to .env"
echo ""
echo "⚠️   The keys/ directory contains sensitive material — it is git-ignored."
echo "     Never commit private keys to version control."
