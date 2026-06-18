###############################################################################
# BookPlus — Infraestructura como código (Terraform)
#
# Aprovisiona una instancia ARM "Always Free" en Oracle Cloud (Ampere A1) con su
# red, y la deja lista con Docker + Docker Compose para desplegar el stack con
# docker-compose.deploy.yml. Reproducible: `terraform apply` recrea todo igual.
###############################################################################

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.0.0"
    }
  }
}

provider "oci" {
  tenancy_ocid     = var.tenancy_ocid
  user_ocid        = var.user_ocid
  fingerprint      = var.fingerprint
  private_key_path = var.private_key_path
  region           = var.region
}

# ── Red ─────────────────────────────────────────────────────────────────────
resource "oci_core_vcn" "bookplus" {
  compartment_id = var.compartment_ocid
  cidr_blocks    = ["10.0.0.0/16"]
  display_name   = "bookplus-vcn"
  dns_label      = "bookplus"
}

resource "oci_core_internet_gateway" "igw" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bookplus.id
  display_name   = "bookplus-igw"
}

resource "oci_core_route_table" "rt" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bookplus.id
  display_name   = "bookplus-rt"
  route_rules {
    destination       = "0.0.0.0/0"
    network_entity_id = oci_core_internet_gateway.igw.id
  }
}

resource "oci_core_security_list" "sl" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bookplus.id
  display_name   = "bookplus-sl"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }

  # SSH, HTTP y HTTPS. (El acceso privado real se hace por Tailscale.)
  dynamic "ingress_security_rules" {
    for_each = var.allowed_ports
    content {
      protocol = "6" # TCP
      source   = "0.0.0.0/0"
      tcp_options {
        min = ingress_security_rules.value
        max = ingress_security_rules.value
      }
    }
  }
}

resource "oci_core_subnet" "public" {
  compartment_id    = var.compartment_ocid
  vcn_id            = oci_core_vcn.bookplus.id
  cidr_block        = "10.0.1.0/24"
  display_name      = "bookplus-public-subnet"
  route_table_id    = oci_core_route_table.rt.id
  security_list_ids = [oci_core_security_list.sl.id]
  dns_label         = "public"
}

# ── Instancia (Ampere A1 — Always Free) ──────────────────────────────────────
resource "oci_core_instance" "app" {
  compartment_id      = var.compartment_ocid
  availability_domain = var.availability_domain
  display_name        = "bookplus-app"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = var.ocpus
    memory_in_gbs = var.memory_in_gbs
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.public.id
    assign_public_ip = true
  }

  source_details {
    source_type = "image"
    source_id   = var.image_ocid # Ubuntu 22.04 ARM
  }

  metadata = {
    ssh_authorized_keys = file(var.ssh_public_key_path)
    user_data           = base64encode(file("${path.module}/cloud-init.yaml"))
  }
}
