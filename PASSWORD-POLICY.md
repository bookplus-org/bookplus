# Política de contraseñas (Bean Validation custom)

Una contraseña larga pero trivial (`password1`) cumple un mínimo de longitud pero es débil.
Para forzar contraseñas robustas en el registro, añadimos una **restricción de validación
personalizada** con **Jakarta Bean Validation**: `@StrongPassword`.

## Cómo funciona

Bean Validation permite definir tus propias anotaciones de validación. `@StrongPassword`:

- es una anotación `@Constraint(validatedBy = StrongPasswordValidator.class)`,
- el validador `StrongPasswordValidator` comprueba que la contraseña tenga **al menos 8
  caracteres** y contenga **mayúscula, minúscula, dígito y símbolo** (carácter no
  alfanumérico).

Se aplica de forma declarativa sobre el campo del DTO de registro:

```java
@NotBlank(message = "Password is required")
@Size(max = 100, ...)        // tope para no procesar entradas enormes
@StrongPassword              // fortaleza
String password
```

Como cualquier restricción de Bean Validation, Spring la evalúa automáticamente al recibir la
petición (`@Valid`); si falla, el cliente recibe un **400** con el mensaje, **sin** que la
lógica de negocio llegue a ejecutarse. El valor nulo/vacío lo gestiona `@NotBlank`, así que
el validador lo deja pasar para no duplicar mensajes.

## Por qué una anotación propia

- **Declarativa y reutilizable**: se aplica con `@StrongPassword` en cualquier campo (registro,
  reset de contraseña, cambio de contraseña) sin repetir lógica.
- **Centralizada**: la política vive en un único sitio; cambiarla (p. ej. exigir 12
  caracteres) es un cambio de una línea que afecta a todos los usos.

## Verificación

`StrongPasswordValidatorTest` (unit, sin Spring) comprueba que acepta contraseñas fuertes y
rechaza las que carecen de mayúscula, minúscula, dígito, símbolo o longitud. Corre en el
`mvn test` normal.

## Siguiente nivel

- Aplicar `@StrongPassword` también al **reset** y **cambio** de contraseña.
- **Pwned Passwords** (HaveIBeenPwned, k-anonymity): rechazar contraseñas que ya aparecen en
  filtraciones conocidas, además de exigir complejidad.
- Mensajes de error **internacionalizados** (i18n) según el idioma del cliente.
