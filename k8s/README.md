# Despliegue en Kubernetes — BookPlus

Manifiestos para desplegar toda la plataforma en un clúster de Kubernetes
(AKS, EKS, GKE o local). Se ofrecen **dos vías**: **Kustomize** (base + overlays)
y un **Helm chart** para las aplicaciones.

## Estructura

```
k8s/
  base/                     # Kustomize base (namespace, apps, infra, observabilidad, ingress)
    apps/                   # 11 apps: Deployment + Service + ConfigMap + Secret
    hpa/                    # HorizontalPodAutoscaler por servicio (solo prod)
    infra/                  # 7 Postgres (StatefulSet+PVC), 3 Redis, Kafka, Elasticsearch, Vault, Schema Registry, MailHog
    observability/          # Prometheus, Alertmanager, Grafana, Loki, Tempo, OTel Collector
    ingress.yaml
    kustomization.yaml
  overlays/
    dev/                    # 1 réplica, sin HPA, CORS local
    prod/                   # 2 réplicas + HPA + CORS de producción
helm/bookplus/              # Helm chart (topología parametrizable de las apps)
generate.py                 # genera los manifiestos base desde docker-compose.full.yml
```

## Requisitos previos

1. **Imágenes** publicadas en el registro (las construye el CI):
   `ghcr.io/bookplus-org/<servicio>:1.0.0`.
2. **Secretos reales** (los manifiestos traen placeholders `REPLACE_WITH_BASE64_VALUE`):
   las claves JWT y contraseñas deben inyectarse con **Sealed Secrets** o el
   **Azure Key Vault CSI driver** (no se commitean secretos reales).
3. Un **Ingress Controller** (p. ej. NGINX) instalado en el clúster.

> Nota de producción: en un entorno real, las bases de datos y Kafka suelen ser
> **servicios administrados** (Azure Database for PostgreSQL, Event Hubs/MSK).
> Aquí se incluyen dentro del clúster para un despliegue autocontenido de demo.

## Desplegar con Kustomize

```bash
# Ver lo que se generaría (sin aplicar)
kubectl kustomize k8s/overlays/dev

# Desarrollo
kubectl apply -k k8s/overlays/dev

# Producción (incluye HPA y 2 réplicas por servicio)
kubectl apply -k k8s/overlays/prod
```

## Desplegar con Helm (solo las aplicaciones)

```bash
helm lint helm/bookplus
helm template bookplus helm/bookplus            # renderiza sin aplicar
helm install bookplus helm/bookplus -n bookplus --create-namespace
# Actualizar / hacer rollback
helm upgrade bookplus helm/bookplus -n bookplus
helm rollback bookplus 1 -n bookplus
```

## Validar los manifiestos (sin clúster)

```bash
# Esquema de Kubernetes
kubectl kustomize k8s/overlays/prod | kubeconform -strict -summary
# o con kustomize standalone:
kustomize build k8s/overlays/prod | kubeconform -strict
```

## Operar y validar el despliegue

```bash
kubectl get pods,svc,hpa -n bookplus
kubectl rollout status deploy/auth-service -n bookplus
kubectl describe pod <pod> -n bookplus         # ver probes y eventos
kubectl top pods -n bookplus                   # consumo real (requiere metrics-server)
kubectl logs -f deploy/order-service -n bookplus

# Probar el autoescalado: generar carga y ver crecer las réplicas
kubectl get hpa -n bookplus -w
```

## Regenerar la base desde el compose

```bash
python3 k8s/generate.py   # reescribe k8s/base/** a partir de docker-compose.full.yml
```
