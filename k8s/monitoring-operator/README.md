# Prometheus Operator (opcional)

Estos recursos (ServiceMonitor, PrometheusRule) son CRDs del **Prometheus Operator**.
Requieren tenerlo instalado (p. ej. kube-prometheus-stack). No van en la base para no
fallar en clústeres sin el operador.

```bash
kubectl apply -k k8s/monitoring-operator
```
