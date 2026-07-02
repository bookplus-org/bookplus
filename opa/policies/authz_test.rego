# Pruebas unitarias de la política (framework de test integrado de OPA: `opa test`).
# Cada test arma un input y comprueba la decisión esperada. Se ejecutan en el CI
# como "policy gate": si una regla se rompe, el pipeline lo detecta.
package bookplus.authz_test

import data.bookplus.authz
import rego.v1

# SUPERADMIN puede cualquier acción.
test_superadmin_allow_all if {
	authz.allow with input as {
		"subject": {"id": "s1", "roles": ["SUPERADMIN"]},
		"action": "refund",
		"resource": {"type": "order", "amount": 999999},
	}
}

# Un usuario puede leer su propio pedido.
test_owner_can_read if {
	authz.allow with input as {
		"subject": {"id": "u1", "roles": ["USER"]},
		"action": "read",
		"resource": {"type": "order", "owner": "u1"},
	}
}

# Un usuario NO puede leer el pedido de otro (IDOR bloqueado por política).
test_non_owner_read_denied if {
	not authz.allow with input as {
		"subject": {"id": "u1", "roles": ["USER"]},
		"action": "read",
		"resource": {"type": "order", "owner": "u2"},
	}
}

# ADMIN puede reembolsar hasta el límite (1000).
test_admin_refund_within_limit if {
	authz.allow with input as {
		"subject": {"id": "a1", "roles": ["ADMIN"]},
		"action": "refund",
		"resource": {"type": "order", "amount": 800},
	}
}

# ADMIN NO puede reembolsar por encima del límite (requiere SUPERADMIN).
test_admin_refund_over_limit_denied if {
	not authz.allow with input as {
		"subject": {"id": "a1", "roles": ["ADMIN"]},
		"action": "refund",
		"resource": {"type": "order", "amount": 5000},
	}
}

# EDITOR puede escribir en el catálogo.
test_editor_can_write_catalog if {
	authz.allow with input as {
		"subject": {"id": "e1", "roles": ["EDITOR"]},
		"action": "write",
		"resource": {"type": "catalog"},
	}
}

# EDITOR NO puede reembolsar.
test_editor_refund_denied if {
	not authz.allow with input as {
		"subject": {"id": "e1", "roles": ["EDITOR"]},
		"action": "refund",
		"resource": {"type": "order", "amount": 10},
	}
}

# REPARTIDOR entrega solo pedidos asignados a él.
test_courier_deliver_assigned if {
	authz.allow with input as {
		"subject": {"id": "rep9", "roles": ["REPARTIDOR"]},
		"action": "deliver",
		"resource": {"type": "order", "assignee": "rep9"},
	}
}

# REPARTIDOR NO puede entregar un pedido asignado a otro.
test_courier_deliver_unassigned_denied if {
	not authz.allow with input as {
		"subject": {"id": "rep9", "roles": ["REPARTIDOR"]},
		"action": "deliver",
		"resource": {"type": "order", "assignee": "rep1"},
	}
}
