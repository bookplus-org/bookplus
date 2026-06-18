# Credenciales del proveedor OCI (ver https://docs.oracle.com/iaas para obtenerlas).
variable "tenancy_ocid"        { type = string }
variable "user_ocid"           { type = string }
variable "fingerprint"         { type = string }
variable "private_key_path"    { type = string }
variable "region"              { type = string  default = "sa-saopaulo-1" }
variable "compartment_ocid"    { type = string }
variable "availability_domain" { type = string }

# Imagen Ubuntu 22.04 para ARM (OCID según región).
variable "image_ocid" { type = string }

# Tamaño de la instancia. El límite Always Free de Ampere A1 es 4 OCPU / 24 GB en total.
variable "ocpus"        { type = number  default = 2 }
variable "memory_in_gbs" { type = number default = 12 }

# Clave pública SSH que se instalará en la instancia.
variable "ssh_public_key_path" {
  type    = string
  default = "~/.ssh/id_ed25519.pub"
}

# Puertos abiertos en el firewall de la nube.
variable "allowed_ports" {
  type    = list(number)
  default = [22, 80, 443]
}
