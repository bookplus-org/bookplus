# Política de autorización de BookPlus como código (Rego / Open Policy Agent).
#
# Modelo ABAC (Attribute-Based Access Control): la decisión se toma a partir de
# ATRIBUTOS del sujeto (roles), la acción y el recurso (dueño, importe, asignado),
# no de reglas cableadas en cada servicio. Así la autorización queda desacoplada
# del código: los servicios consultan a OPA y OPA responde allow/deny.
#
# Entrada esperada (input):
#   {
#     "subject":  { "id": "u1", "roles": ["ADMIN"] },
#     "action":   "refund",                 # read | write | refund | deliver
#     "resource": { "type":"order", "owner":"u1", "amount":500, "assignee":"rep9" }
#   }
package bookplus.authz

import rego.v1

# Por defecto se DENIEGA (fail-safe). Solo se permite si alguna regla lo concede.
default allow := false

# ── Helpers ────────────────────────────────────────────────────────────────
has_role(r) if r in input.subject.roles

# Límite de reembolso que un ADMIN puede aprobar sin ser SUPERADMIN.
admin_refund_limit := 1000

# ── Reglas de concesión ──────────────────────────────────────────────────────

# SUPERADMIN puede todo.
allow if has_role("SUPERADMIN")

# Cualquiera puede LEER su propio recurso (ownership).
allow if {
	input.action == "read"
	input.resource.owner == input.subject.id
}

# El dueño puede MODIFICAR su propio recurso.
allow if {
	input.action == "write"
	input.resource.owner == input.subject.id
}

# EDITOR/ADMIN pueden escribir en el catálogo (recurso de tipo "catalog").
allow if {
	input.action == "write"
	input.resource.type == "catalog"
	some r in {"EDITOR", "ADMIN"}
	has_role(r)
}

# ADMIN puede aprobar reembolsos hasta el límite; por encima, solo SUPERADMIN
# (cubierto por la regla de arriba). Regla ABAC clásica sobre el importe.
allow if {
	input.action == "refund"
	has_role("ADMIN")
	input.resource.amount <= admin_refund_limit
}

# REPARTIDOR puede marcar entregas SOLO de los pedidos que tiene asignados.
allow if {
	input.action == "deliver"
	has_role("REPARTIDOR")
	input.resource.assignee == input.subject.id
}

# Motivo de la denegación (útil para logs/depuración del punto de aplicación).
reason := "allowed" if allow

reason := "denied: subject lacks required attributes/role for action" if not allow
