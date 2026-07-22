# Gestión de secretos en Kubernetes

Los manifiestos de `k8s/base` traen los `Secret` con **placeholders**
(`REPLACE_WITH_BASE64_VALUE`). Los secretos reales NUNCA se commitean en claro.
Dos formas soportadas:

## Opción A — Sealed Secrets (GitOps, secretos cifrados en el repo)

Instalar el controlador y `kubeseal` una vez:
```bash
# Controlador en el clúster (ejemplo con Helm)
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm install sealed-secrets sealed-secrets/sealed-secrets -n kube-system
# Cliente kubeseal (descarga desde github.com/bitnami-labs/sealed-secrets/releases)
```

Sellar y aplicar:
```bash
# 1) Copia la plantilla y rellena los valores REALES (base64 de las claves JWT, etc.)
cp secrets.example.yaml secrets.filled.yaml   # edita este archivo (NO lo commitees)
# 2) Sella -> sealed-secrets.yaml (ESTE sí se commitea, va cifrado)
./seal.sh secrets.filled.yaml sealed-secrets.yaml
# 3) Aplica al clúster
kubectl apply -f sealed-secrets.yaml
```
El controlador descifra el `SealedSecret` y crea el `Secret` real dentro del clúster.
Nadie fuera del clúster puede leerlo.

## Opción B — Azure Key Vault (CSI driver) — recomendado en AKS

Los secretos viven en **Azure Key Vault** y se montan/sincronizan en el clúster:
```bash
az keyvault create -g rg-bookplus -n kv-bookplus
az keyvault secret set --vault-name kv-bookplus -n auth-db-pass   --value '<valor>'
az keyvault secret set --vault-name kv-bookplus -n jwt-private-key --file jwt_private.b64
az aks enable-addons -g rg-bookplus -n aks-bookplus --addons azure-keyvault-secrets-provider

kubectl apply -f azure-keyvault/secretproviderclass-auth.yaml
kubectl apply -f azure-keyvault/deployment-patch-auth.yaml   # o aplica el patch con kustomize
```
Con `secretObjects`, el CSI crea el `Secret auth-service-secret` automáticamente,
así que el `envFrom: secretRef` de los Deployments no cambia.

## Generar las claves JWT (base64)

```bash
# Las claves RSA del JWT en base64 (una sola línea), como espera la app:
base64 -w0 jwt_private.pem   # -> APP_SECURITY_JWT_PRIVATE_KEY
base64 -w0 jwt_public.pem    # -> *_JWT_PUBLIC_KEY_BASE64
```
