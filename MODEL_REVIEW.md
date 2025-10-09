# Revisión del modelo de dominio

Esta nota resume mi apreciación sobre las clases ubicadas en `src/main/java/co/edu/uniquindio/oldbaker/model` y las ideas que expresan acerca del dominio de Old Baker.

## Aspectos positivos

* **Entidades claras y con nombres descriptivos.** Las clases representan conceptos habituales de un sistema de panadería: productos, insumos, proveedores, pedidos, pagos y usuarios. Esto facilita entender los casos de uso principales desde el modelo. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/Producto.java†L6-L22】【F:src/main/java/co/edu/uniquindio/oldbaker/model/PedidoInsumo.java†L9-L34】
* **Relaciones básicas entre entidades.** Se definen asociaciones entre pedidos, detalles, insumos y proveedores que permiten reconstruir el ciclo de aprovisionamiento. Estas relaciones servirán como punto de partida para consultas más completas. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/DetalleProveedorPedido.java†L18-L35】【F:src/main/java/co/edu/uniquindio/oldbaker/model/InsumoProveedor.java†L24-L35】
* **Integración con Spring Security en el modelo de usuario.** `Usuario` implementa `UserDetails` y define roles y tipos de autenticación, lo que muestra la intención de integrar el modelo con el subsistema de seguridad. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/Usuario.java†L6-L96】

## Oportunidades de mejora

* **Tipos de datos y restricciones.** Algunos atributos deberían usar tipos más específicos. Por ejemplo, `Producto` guarda la fecha de vencimiento como `String` y los importes monetarios como `Double`; sería preferible usar `LocalDate` y `BigDecimal` para evitar errores de formato y precisión. Además, faltan anotaciones de validación (`@NotNull`, `@Size`, etc.) en varias propiedades clave. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/Producto.java†L16-L21】【F:src/main/java/co/edu/uniquindio/oldbaker/model/PagoProveedor.java†L14-L17】
* **Uso de `@Data` en entidades JPA.** Lombok genera `equals`, `hashCode` y `toString` que consideran todas las relaciones, lo que puede provocar ciclos o `LazyInitializationException`. Sería más seguro usar `@Getter`/`@Setter` y definir manualmente `equals` y `hashCode` según la clave natural o el identificador. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/InsumoProveedor.java†L10-L35】
* **Cardinalidades cuestionables.** `Insumo` está ligado en una relación `@OneToOne` con `InsumoProveedor`, lo que impide reutilizar un insumo comprado a varios proveedores. Probablemente convendría un esquema `@ManyToMany` con atributos adicionales (precio, disponibilidad) en la tabla intermedia. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/Insumo.java†L17-L24】
* **Modelo incompleto del inventario.** Actualmente no hay una entidad que refleje existencias por producto terminado ni historial de movimientos. Para cubrir procesos de producción y venta harían falta tablas adicionales o atributos en `Producto` y `Receta`. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/Receta.java†L12-L21】
* **Ausencia de trazabilidad y estados detallados.** Los pedidos solo cuentan con un enum de estado genérico; podría ampliarse para registrar fechas de aprobación, recepción o pago, así como el usuario responsable de cada cambio. 【F:src/main/java/co/edu/uniquindio/oldbaker/model/PedidoInsumo.java†L21-L27】【F:src/main/java/co/edu/uniquindio/oldbaker/model/EstadoPedido.java†L3-L7】

## Recomendaciones generales

1. Revisar las anotaciones de validación y los tipos de datos para alinearlos con las reglas del negocio (por ejemplo, usar `@DecimalMin`, `@Positive` y `@PastOrPresent`).
2. Reducir el uso de `@Data` en entidades JPA para controlar los métodos generados y evitar efectos secundarios en sesiones de Hibernate.
3. Repensar las asociaciones entre insumos y proveedores para soportar múltiples proveedores por insumo y poder registrar cambios de precio en el tiempo.
4. Incorporar entidades o atributos que capten el flujo de producción (ej. consumo de insumos por recetas, lotes producidos, stock final).
5. Documentar mediante comentarios o diagramas las decisiones clave del modelo para facilitar su mantenimiento.

En general, el modelo constituye una base clara pero todavía simplificada; reforzarlo con validaciones, cardinalidades más flexibles y trazabilidad permitirá que represente mejor los procesos reales de la panadería.
