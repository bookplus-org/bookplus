#!/usr/bin/env python3
"""Genera los manifiestos Kubernetes de BookPlus (base Kustomize + HPA) desde
docker-compose.full.yml, para mantenerlos fieles a la configuracion real."""
import os, re, yaml, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
COMPOSE = os.path.join(ROOT, "docker-compose.full.yml")
MON = os.path.join(ROOT, "monitoring")
BASE = os.path.join(ROOT, "k8s", "base")
NS = "bookplus"; REGISTRY = "ghcr.io/bookplus-org"; TAG = "1.0.0"

compose = yaml.safe_load(open(COMPOSE)); services = compose["services"]
for sub in ("apps","hpa","infra","observability"):
    d=os.path.join(BASE,sub)
    if os.path.isdir(d): shutil.rmtree(d)

def envmap(name):
    e = services.get(name, {}).get("environment", {})
    if isinstance(e, list): e={x.split("=")[0]:(x.split("=",1)[1] if "=" in x else "") for x in e}
    return e or {}
def resolve(v, secret=False):
    v=str(v)
    m=re.fullmatch(r"\$\{([^:}]+):-(.*)\}", v)
    if m: return m.group(2)
    if re.fullmatch(r"\$\{[^}]+\}", v): return "REPLACE_WITH_BASE64_VALUE" if secret else ""
    return v
def is_secret(k):
    u=k.upper(); return any(h in u for h in ("PASS","PRIVATE_KEY","PUBLIC_KEY","SECRET","TOKEN"))
def L(app): return {"app.kubernetes.io/name":app,"app.kubernetes.io/part-of":"bookplus"}
def write(path, docs):
    full=os.path.join(BASE,path); os.makedirs(os.path.dirname(full),exist_ok=True)
    open(full,"w").write("\n---\n".join(yaml.safe_dump(d,sort_keys=False) for d in docs))

resources=[]; hpa_files=[]

# namespace
write("namespace.yaml",[{"apiVersion":"v1","kind":"Namespace","metadata":{"name":NS,"labels":{"app.kubernetes.io/part-of":"bookplus"}}}])
resources.append("namespace.yaml")

# ---------- Aplicaciones ----------
APPS={"api-gateway":(8080,True),"auth-service":(8081,True),"catalog-service":(8082,True),
"inventory-service":(8083,True),"cart-service":(8084,True),"order-service":(8085,True),
"payment-service":(8086,True),"notification-service":(8087,True),"report-service":(8088,True),
"admin-bff":(8089,True),"frontend":(80,False)}
for name,(port,spring) in APPS.items():
    env=envmap(name)
    cfg={k:resolve(v) for k,v in env.items() if not is_secret(k)}; cfg.pop("SERVER_PORT",None)
    sec={k:resolve(v,True) for k,v in env.items() if is_secret(k)}
    docs=[]
    if cfg: docs.append({"apiVersion":"v1","kind":"ConfigMap","metadata":{"name":f"{name}-config","labels":L(name)},"data":{k:str(v) for k,v in cfg.items()}})
    if sec: docs.append({"apiVersion":"v1","kind":"Secret","metadata":{"name":f"{name}-secret","labels":L(name)},"type":"Opaque","stringData":{k:str(v) for k,v in sec.items()}})
    ports=[{"containerPort":port,"name":"http"}]
    if name=="inventory-service": ports.append({"containerPort":9090,"name":"grpc"})
    hp="/actuator/health" if spring else "/"
    c={"name":name,"image":f"{REGISTRY}/{name}:{TAG}","imagePullPolicy":"IfNotPresent","ports":ports,
       "envFrom":([{"configMapRef":{"name":f"{name}-config"}}] if cfg else [])+([{"secretRef":{"name":f"{name}-secret"}}] if sec else []),
       "resources":{"requests":{"cpu":"200m","memory":"512Mi" if spring else "64Mi"},"limits":{"cpu":"1","memory":"1Gi" if spring else "128Mi"}},
       "startupProbe":{"httpGet":{"path":hp,"port":port},"failureThreshold":30,"periodSeconds":5},
       "livenessProbe":{"httpGet":{"path":hp,"port":port},"periodSeconds":15,"initialDelaySeconds":10},
       "readinessProbe":{"httpGet":{"path":hp,"port":port},"periodSeconds":10,"initialDelaySeconds":10}}
    docs.append({"apiVersion":"apps/v1","kind":"Deployment","metadata":{"name":name,"labels":L(name)},
       "spec":{"replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":name}},
               "template":{"metadata":{"labels":L(name)},"spec":{"containers":[c]}}}})
    sp=[{"port":port,"targetPort":port,"name":"http"}]
    if name=="inventory-service": sp.append({"port":9090,"targetPort":9090,"name":"grpc"})
    docs.append({"apiVersion":"v1","kind":"Service","metadata":{"name":name,"labels":L(name)},"spec":{"selector":{"app.kubernetes.io/name":name},"ports":sp}})
    write(f"apps/{name}.yaml",docs); resources.append(f"apps/{name}.yaml")
    if spring:
        write(f"hpa/{name}.yaml",[{"apiVersion":"autoscaling/v2","kind":"HorizontalPodAutoscaler",
          "metadata":{"name":f"{name}-hpa","labels":L(name)},
          "spec":{"scaleTargetRef":{"apiVersion":"apps/v1","kind":"Deployment","name":name},"minReplicas":2,"maxReplicas":8,
                  "metrics":[{"type":"Resource","resource":{"name":"cpu","target":{"type":"Utilization","averageUtilization":70}}}]}}])
        hpa_files.append(f"{name}.yaml")

# ---------- Postgres (StatefulSet + PVC) ----------
for svc in ["auth","catalog","inventory","order","payment","notification","report"]:
    name=f"{svc}-postgres"; e=envmap(name)
    docs=[
      {"apiVersion":"v1","kind":"Secret","metadata":{"name":f"{name}-secret","labels":L(name)},"type":"Opaque","stringData":{"POSTGRES_PASSWORD":e.get("POSTGRES_PASSWORD",f"{svc}_pass")}},
      {"apiVersion":"v1","kind":"Service","metadata":{"name":name,"labels":L(name)},"spec":{"clusterIP":"None","selector":{"app.kubernetes.io/name":name},"ports":[{"port":5432,"targetPort":5432,"name":"pg"}]}},
      {"apiVersion":"apps/v1","kind":"StatefulSet","metadata":{"name":name,"labels":L(name)},
       "spec":{"serviceName":name,"replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":name}},
         "template":{"metadata":{"labels":L(name)},"spec":{"containers":[{"name":"postgres","image":"postgres:16-alpine",
             "ports":[{"containerPort":5432,"name":"pg"}],
             "env":[{"name":"POSTGRES_DB","value":e.get("POSTGRES_DB",f"{svc}_db")},{"name":"POSTGRES_USER","value":e.get("POSTGRES_USER",f"{svc}_user")},
                    {"name":"POSTGRES_PASSWORD","valueFrom":{"secretKeyRef":{"name":f"{name}-secret","key":"POSTGRES_PASSWORD"}}}],
             "volumeMounts":[{"name":"data","mountPath":"/var/lib/postgresql/data"}],
             "readinessProbe":{"exec":{"command":["pg_isready","-U",e.get("POSTGRES_USER",f"{svc}_user")]},"periodSeconds":10},
             "resources":{"requests":{"cpu":"100m","memory":"256Mi"},"limits":{"cpu":"500m","memory":"512Mi"}}}]}},
         "volumeClaimTemplates":[{"metadata":{"name":"data"},"spec":{"accessModes":["ReadWriteOnce"],"resources":{"requests":{"storage":"2Gi"}}}}]}}]
    write(f"infra/{name}.yaml",docs); resources.append(f"infra/{name}.yaml")

# ---------- Redis ----------
for name in ["gateway-redis","catalog-redis","cart-redis"]:
    docs=[
      {"apiVersion":"apps/v1","kind":"Deployment","metadata":{"name":name,"labels":L(name)},
       "spec":{"replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":name}},
         "template":{"metadata":{"labels":L(name)},"spec":{"containers":[{"name":"redis","image":"redis:7.2-alpine",
             "args":["redis-server","--save","","--loglevel","warning"],"ports":[{"containerPort":6379,"name":"redis"}],
             "resources":{"requests":{"cpu":"50m","memory":"64Mi"},"limits":{"cpu":"250m","memory":"128Mi"}}}]}}}},
      {"apiVersion":"v1","kind":"Service","metadata":{"name":name,"labels":L(name)},"spec":{"selector":{"app.kubernetes.io/name":name},"ports":[{"port":6379,"targetPort":6379,"name":"redis"}]}}]
    write(f"infra/{name}.yaml",docs); resources.append(f"infra/{name}.yaml")

# ---------- Kafka (KRaft, 1 broker) ----------
ke=envmap("kafka")
write("infra/kafka.yaml",[
  {"apiVersion":"v1","kind":"Service","metadata":{"name":"kafka","labels":L("kafka")},"spec":{"clusterIP":"None","selector":{"app.kubernetes.io/name":"kafka"},"ports":[{"port":9092,"name":"broker"},{"port":9093,"name":"controller"}]}},
  {"apiVersion":"apps/v1","kind":"StatefulSet","metadata":{"name":"kafka","labels":L("kafka")},
   "spec":{"serviceName":"kafka","replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":"kafka"}},
     "template":{"metadata":{"labels":L("kafka")},"spec":{"containers":[{"name":"kafka","image":"apache/kafka:3.8.0",
        "ports":[{"containerPort":9092,"name":"broker"},{"containerPort":9093,"name":"controller"}],
        "env":[{"name":k,"value":str(v)} for k,v in ke.items()],
        "volumeMounts":[{"name":"data","mountPath":"/var/lib/kafka/data"}],
        "resources":{"requests":{"cpu":"250m","memory":"512Mi"},"limits":{"cpu":"1","memory":"1Gi"}}}]}},
     "volumeClaimTemplates":[{"metadata":{"name":"data"},"spec":{"accessModes":["ReadWriteOnce"],"resources":{"requests":{"storage":"3Gi"}}}}]}}])
resources.append("infra/kafka.yaml")

# ---------- Elasticsearch ----------
ese=envmap("elasticsearch")
write("infra/elasticsearch.yaml",[
  {"apiVersion":"v1","kind":"Service","metadata":{"name":"elasticsearch","labels":L("elasticsearch")},"spec":{"selector":{"app.kubernetes.io/name":"elasticsearch"},"ports":[{"port":9200,"targetPort":9200,"name":"http"}]}},
  {"apiVersion":"apps/v1","kind":"StatefulSet","metadata":{"name":"elasticsearch","labels":L("elasticsearch")},
   "spec":{"serviceName":"elasticsearch","replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":"elasticsearch"}},
     "template":{"metadata":{"labels":L("elasticsearch")},"spec":{"containers":[{"name":"elasticsearch","image":"docker.elastic.co/elasticsearch/elasticsearch:8.14.0",
        "ports":[{"containerPort":9200,"name":"http"}],
        "env":[{"name":k,"value":str(v)} for k,v in ese.items()],
        "volumeMounts":[{"name":"data","mountPath":"/usr/share/elasticsearch/data"}],
        "resources":{"requests":{"cpu":"250m","memory":"1Gi"},"limits":{"cpu":"1","memory":"1536Mi"}}}]}},
     "volumeClaimTemplates":[{"metadata":{"name":"data"},"spec":{"accessModes":["ReadWriteOnce"],"resources":{"requests":{"storage":"3Gi"}}}}]}}])
resources.append("infra/elasticsearch.yaml")

# ---------- Vault (dev), Schema Registry, MailHog ----------
write("infra/vault.yaml",[
  {"apiVersion":"v1","kind":"Service","metadata":{"name":"vault","labels":L("vault")},"spec":{"selector":{"app.kubernetes.io/name":"vault"},"ports":[{"port":8200,"targetPort":8200,"name":"http"}]}},
  {"apiVersion":"apps/v1","kind":"Deployment","metadata":{"name":"vault","labels":L("vault")},
   "spec":{"replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":"vault"}},
     "template":{"metadata":{"labels":L("vault")},"spec":{"containers":[{"name":"vault","image":"hashicorp/vault:1.17","args":["server","-dev"],
        "ports":[{"containerPort":8200,"name":"http"}],
        "env":[{"name":"VAULT_DEV_ROOT_TOKEN_ID","valueFrom":{"secretKeyRef":{"name":"vault-secret","key":"token"}}},
               {"name":"VAULT_DEV_LISTEN_ADDRESS","value":"0.0.0.0:8200"},{"name":"VAULT_ADDR","value":"http://0.0.0.0:8200"}],
        "securityContext":{"capabilities":{"add":["IPC_LOCK"]}},
        "resources":{"requests":{"cpu":"50m","memory":"128Mi"},"limits":{"cpu":"250m","memory":"256Mi"}}}]}}}},
  {"apiVersion":"v1","kind":"Secret","metadata":{"name":"vault-secret","labels":L("vault")},"type":"Opaque","stringData":{"token":"bookplus-root"}}])
resources.append("infra/vault.yaml")

write("infra/schema-registry.yaml",[
  {"apiVersion":"v1","kind":"Service","metadata":{"name":"schema-registry","labels":L("schema-registry")},"spec":{"selector":{"app.kubernetes.io/name":"schema-registry"},"ports":[{"port":8080,"targetPort":8080,"name":"http"}]}},
  {"apiVersion":"apps/v1","kind":"Deployment","metadata":{"name":"schema-registry","labels":L("schema-registry")},
   "spec":{"replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":"schema-registry"}},
     "template":{"metadata":{"labels":L("schema-registry")},"spec":{"containers":[{"name":"apicurio","image":"apicurio/apicurio-registry-mem:2.6.5.Final",
        "ports":[{"containerPort":8080,"name":"http"}],"env":[{"name":"QUARKUS_PROFILE","value":"prod"}],
        "resources":{"requests":{"cpu":"100m","memory":"256Mi"},"limits":{"cpu":"500m","memory":"512Mi"}}}]}}}}])
resources.append("infra/schema-registry.yaml")

write("infra/mailhog.yaml",[
  {"apiVersion":"v1","kind":"Service","metadata":{"name":"mailhog","labels":L("mailhog")},"spec":{"selector":{"app.kubernetes.io/name":"mailhog"},"ports":[{"port":1025,"targetPort":1025,"name":"smtp"},{"port":8025,"targetPort":8025,"name":"ui"}]}},
  {"apiVersion":"apps/v1","kind":"Deployment","metadata":{"name":"mailhog","labels":L("mailhog")},
   "spec":{"replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":"mailhog"}},
     "template":{"metadata":{"labels":L("mailhog")},"spec":{"containers":[{"name":"mailhog","image":"mailhog/mailhog:latest",
        "ports":[{"containerPort":1025,"name":"smtp"},{"containerPort":8025,"name":"ui"}],
        "resources":{"requests":{"cpu":"25m","memory":"64Mi"},"limits":{"cpu":"100m","memory":"128Mi"}}}]}}}}])
resources.append("infra/mailhog.yaml")

# ---------- Observabilidad (con ConfigMaps desde monitoring/) ----------
def readf(rel):
    p=os.path.join(MON,rel)
    return open(p).read() if os.path.isfile(p) else None

def obs_deploy(name,image,port,args=None,cmvol=None,extra_ports=None,env=None):
    ports=[{"containerPort":port,"name":"http"}]+(extra_ports or [])
    c={"name":name,"image":image,"ports":ports,"resources":{"requests":{"cpu":"50m","memory":"128Mi"},"limits":{"cpu":"500m","memory":"512Mi"}}}
    if args: c["args"]=args
    if env: c["env"]=env
    vols=[]; 
    if cmvol:
        c["volumeMounts"]=[{"name":"config","mountPath":cmvol["mount"]}]
        vols=[{"name":"config","configMap":{"name":cmvol["cm"]}}]
    dep={"apiVersion":"apps/v1","kind":"Deployment","metadata":{"name":name,"labels":L(name)},
       "spec":{"replicas":1,"selector":{"matchLabels":{"app.kubernetes.io/name":name}},
         "template":{"metadata":{"labels":L(name)},"spec":{"containers":[c],**({"volumes":vols} if vols else {})}}}}
    svc={"apiVersion":"v1","kind":"Service","metadata":{"name":name,"labels":L(name)},"spec":{"selector":{"app.kubernetes.io/name":name},"ports":[{"port":p["containerPort"],"targetPort":p["containerPort"],"name":p["name"]} for p in ports]}}
    return dep,svc

# prometheus (+ configmap prometheus.yml y alert-rules.yml)
prom_cm={"apiVersion":"v1","kind":"ConfigMap","metadata":{"name":"prometheus-config","labels":L("prometheus")},"data":{}}
if readf("prometheus.yml"): prom_cm["data"]["prometheus.yml"]=readf("prometheus.yml")
if readf("alert-rules.yml"): prom_cm["data"]["alert-rules.yml"]=readf("alert-rules.yml")
dep,svc=obs_deploy("prometheus","prom/prometheus:v2.54.1",9090,args=["--config.file=/etc/prometheus/prometheus.yml"],cmvol={"cm":"prometheus-config","mount":"/etc/prometheus"})
write("observability/prometheus.yaml",[prom_cm,dep,svc]); resources.append("observability/prometheus.yaml")

# alertmanager
am_cm={"apiVersion":"v1","kind":"ConfigMap","metadata":{"name":"alertmanager-config","labels":L("alertmanager")},"data":{"alertmanager.yml":readf("alertmanager/alertmanager.yml") or "route: { receiver: default }\nreceivers: [{name: default}]\n"}}
dep,svc=obs_deploy("alertmanager","prom/alertmanager:v0.27.0",9093,args=["--config.file=/etc/alertmanager/alertmanager.yml"],cmvol={"cm":"alertmanager-config","mount":"/etc/alertmanager"})
write("observability/alertmanager.yaml",[am_cm,dep,svc]); resources.append("observability/alertmanager.yaml")

# otel-collector
otel_cm={"apiVersion":"v1","kind":"ConfigMap","metadata":{"name":"otel-config","labels":L("otel-collector")},"data":{"config.yaml":readf("otel-collector-config.yml") or "receivers: {}\n"}}
dep,svc=obs_deploy("otel-collector","otel/opentelemetry-collector-contrib:0.108.0",4318,args=["--config=/etc/otelcol/config.yaml"],cmvol={"cm":"otel-config","mount":"/etc/otelcol"},extra_ports=[{"containerPort":4317,"name":"grpc"}])
write("observability/otel-collector.yaml",[otel_cm,dep,svc]); resources.append("observability/otel-collector.yaml")

# tempo
tempo_cm={"apiVersion":"v1","kind":"ConfigMap","metadata":{"name":"tempo-config","labels":L("tempo")},"data":{"tempo.yaml":readf("tempo.yml") or "server: { http_listen_port: 3200 }\n"}}
dep,svc=obs_deploy("tempo","grafana/tempo:2.6.0",3200,args=["-config.file=/etc/tempo/tempo.yaml"],cmvol={"cm":"tempo-config","mount":"/etc/tempo"})
write("observability/tempo.yaml",[tempo_cm,dep,svc]); resources.append("observability/tempo.yaml")

# loki y grafana (config por defecto)
dep,svc=obs_deploy("loki","grafana/loki:3.1.1",3100)
write("observability/loki.yaml",[dep,svc]); resources.append("observability/loki.yaml")
dep,svc=obs_deploy("grafana","grafana/grafana:11.2.0",3000,env=[{"name":"GF_SECURITY_ADMIN_USER","value":"admin"},{"name":"GF_SECURITY_ADMIN_PASSWORD","valueFrom":{"secretKeyRef":{"name":"grafana-secret","key":"password"}}}])
write("observability/grafana.yaml",[dep,svc,{"apiVersion":"v1","kind":"Secret","metadata":{"name":"grafana-secret","labels":L("grafana")},"type":"Opaque","stringData":{"password":"admin"}}]); resources.append("observability/grafana.yaml")

# ---------- Ingress ----------
write("ingress.yaml",[{"apiVersion":"networking.k8s.io/v1","kind":"Ingress","metadata":{"name":"bookplus","labels":{"app.kubernetes.io/part-of":"bookplus"},"annotations":{"nginx.ingress.kubernetes.io/ssl-redirect":"true"}},
  "spec":{"ingressClassName":"nginx","rules":[{"host":"bookplus.example","http":{"paths":[
      {"path":"/api","pathType":"Prefix","backend":{"service":{"name":"api-gateway","port":{"number":8080}}}},
      {"path":"/graphql","pathType":"Prefix","backend":{"service":{"name":"api-gateway","port":{"number":8080}}}},
      {"path":"/","pathType":"Prefix","backend":{"service":{"name":"frontend","port":{"number":80}}}}]}}]}}])
resources.append("ingress.yaml")

# ---------- kustomization (base) ----------
open(os.path.join(BASE,"kustomization.yaml"),"w").write(yaml.safe_dump({
  "apiVersion":"kustomize.config.k8s.io/v1beta1","kind":"Kustomization","namespace":NS,
  "commonLabels":{"app.kubernetes.io/managed-by":"kustomize"},"resources":resources},sort_keys=False))

# ---------- kustomization (base/hpa) ----------
open(os.path.join(BASE,"hpa","kustomization.yaml"),"w").write(yaml.safe_dump({
  "apiVersion":"kustomize.config.k8s.io/v1beta1","kind":"Kustomization","namespace":NS,"resources":sorted(hpa_files)},sort_keys=False))

print("OK. recursos base:",len(resources),"| HPAs:",len(hpa_files))
