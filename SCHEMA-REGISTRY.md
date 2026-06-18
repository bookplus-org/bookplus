# Schema Registry + Avro (gobierno de eventos)

Hoy los eventos de Kafka en BookPlus viajan como **JSON libre**: no hay un contrato formal
de su estructura, y un cambio en el productor puede romper a los consumidores en silencio.
Un **Schema Registry** resuelve esto: cada evento tiene un **esquema versionado** (Avro) que
se registra y valida, y el registry **rechaza** cambios incompatibles. Es gobierno de datos
de eventos, estándar en empresas con muchos productores/consumidores (y muy presente en banca).

## Qué se ha añadido

- **`schema-registry`** (Apicurio Registry, Apache 2.0) en `docker-compose.full.yml`.
  UI/API en `http://localhost:8090`.
- **`schemas/avro/cart-checked-out.avsc`** — el esquema Avro del evento `cart.checked-out`
  (cart lo produce, order lo consume), con tipos precisos: `decimal` para los importes,
  uniones `["null","string"]` para los campos opcionales, records anidados (Money, dirección)
  y un array de ítems.
- **`schemas/register-schemas.sh`** — registra el esquema y fija la regla de compatibilidad
  **BACKWARD** (un esquema nuevo debe poder leer datos producidos con el anterior).

## Cómo registrarlo

```bash
docker compose -f docker-compose.full.yml up -d schema-registry
sh schemas/register-schemas.sh
# Ver en http://localhost:8090/ui
```

## Avro vs JSON

- **Compacto y rápido**: Avro es binario; ocupa menos y serializa más rápido que JSON.
- **Tipado fuerte**: el esquema documenta y valida la estructura (tipos, opcionales, defaults).
- **Evolución segura**: con reglas de compatibilidad, añadir un campo con `default` es
  compatible; quitar un campo obligatorio o cambiar un tipo se **rechaza** antes de romper nada.

## Ruta de migración (cuando se adopte en la serialización)

1. Añadir el `avro-maven-plugin` para generar las clases Java desde los `.avsc`.
2. Cambiar el productor (cart) a `KafkaAvroSerializer`/serde de Apicurio, apuntando al registry.
3. Cambiar el consumidor (order) al deserializador Avro correspondiente.
4. Probar el round-trip con `MockSchemaRegistryClient` en tests unitarios y, en runtime, la
   evolución de esquemas contra el registry.

Este repositorio deja el registry y el esquema listos (gobierno), de modo que el cambio de
serialización es un paso acotado y verificable con el stack en marcha.
