output "public_ip" {
  description = "IP pública de la instancia BookPlus"
  value       = oci_core_instance.app.public_ip
}

output "ssh_command" {
  description = "Comando para conectarse por SSH"
  value       = "ssh ubuntu@${oci_core_instance.app.public_ip}"
}
