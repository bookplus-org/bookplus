# Terraform — Infraestructura de BookPlus (Oracle Cloud)

Aprovisiona, de forma reproducible, el servidor donde corre el stack: una instancia
ARM **Always Free** (Ampere A1) en Oracle Cloud, su red (VCN, subred, gateway, firewall)
y, vía `cloud-init`, Docker + Docker Compose + Tailscale ya instalados.

## Uso

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # rellena tus OCIDs y credenciales
terraform init       # descarga el provider oci
terraform plan        # revisa lo que se va a crear
terraform apply       # crea la infraestructura
terraform output ssh_command   # cómo conectarte
```

Para destruir todo: `terraform destroy`.

## Qué crea

| Recurso | Descripción |
|---|---|
| `oci_core_vcn` | Red virtual 10.0.0.0/16 |
| `oci_core_internet_gateway` + `route_table` | Salida a internet |
| `oci_core_security_list` | Firewall: 22 (SSH), 80, 443 |
| `oci_core_subnet` | Subred pública con IP automática |
| `oci_core_instance` | VM ARM (2 OCPU / 12 GB por defecto; el límite Free total es 4/24) |

## Después del apply

La instancia arranca con Docker listo. Para desplegar BookPlus:

1. `scp docker-compose.deploy.yml .env ubuntu@<IP>:/opt/bookplus/`
2. `ssh ubuntu@<IP>` → `docker login ghcr.io` (token de GHCR)
3. `cd /opt/bookplus && docker compose -f docker-compose.deploy.yml up -d`
4. Acceso privado: `sudo tailscale up`

> Nota: el `terraform.tfvars` contiene credenciales — está pensado para quedar fuera de Git.
