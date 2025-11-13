# OldBaker API

Este documento describe los endpoints disponibles, los JSON de entrada y las salidas esperadas. Las rutas incluyen los endpoints nuevos para reparto y los existentes para checkout, estados y seguimiento.

Base URL por defecto (local):
- HTTPS: https://localhost:8443
- HTTP (si está habilitado): http://localhost:8080

Context-path: ninguno (por defecto).

Autenticación: según configuración de seguridad, varios endpoints requieren usuario autenticado y tienen reglas de rol (CLIENTE, ADMINISTRADOR, AUXILIAR, DELIVERY).

---

## Autenticación

- POST /api/auth/login
  - Body: credenciales (según implementación del proyecto)
  - Respuesta: token/usuario (según implementación)

- POST /api/user/logout
  - Body:
    {
      "token": "..."
    }
  - Respuesta: 200 OK

CORS: Permitir orígenes:
- https://old-baker-front.vercel.app
- https://localhost:4200
- http://localhost:4200
- https://www.oldbaker.shop

---

## Carrito (Cart)

- GET /api/user/{idUsuario}/cart
  - Descripción: Obtiene el carrito del usuario.
  - Respuesta (200):
    {
      "userId": 123,
      "items": [
        { "productId": 1, "quantity": 2, "selected": true }
      ]
    }

- PUT /api/user/{idUsuario}/cart
  - Descripción: Reemplaza los ítems del carrito del usuario.
  - Body:
    {
      "userId": 123,
      "items": [
        { "productId": 1, "quantity": 2, "selected": true },
        { "productId": 5, "quantity": 1, "selected": false }
      ]
    }
  - Respuesta (200): igual estructura que GET.

Notas: El campo exacto del DTO puede variar según CartDTO e ItemCartDTO del proyecto.

---

## Checkout y Órdenes

- POST /api/orders/checkout (requiere usuario autenticado)
  - Valida stock, crea la orden (paymentStatus=PENDING) y crea preferencia de pago en Mercado Pago.
  - Body:
    {
      "items": [ { "productId": 1, "quantity": 2 }, { "productId": 5, "quantity": 1 } ],
      "payerEmail": "cliente@correo.com"
    }
  - Respuesta (200):
    {
      "initPoint": "https://mercadopago.com/checkout/abc",
      "preferenceId": "pref_123"
    }
  - Errores: 400 (validación), 401 (no autenticado), 500 (error interno)

- GET /api/orders/status?externalReference=REF
  - Descripción: Consulta el estado de una orden por su external_reference (devuelto indirectamente vía Mercado Pago / almacenado en BD).
  - Respuesta (200):
    {
      "orderId": 1,
      "status": "PENDING",               // compat: derivado de payment/delivery
      "paymentStatus": "PENDING|IN_PROCESS|PAID|FAILED|CANCELLED",
      "deliveryStatus": "CONFIRMED|PREPARING|DISPATCHED|DELIVERED|null",
      "total": 35000.00,
      "paymentId": "pay_123",
      "fechaCreacion": "2025-11-12T10:20:30",
      "trackingCode": "TRK-001",
      "fechaEntregaEstimada": "2025-11-15T09:00:00",
      "items": [
        { "producto": "Pan integral", "cantidad": 2, "precioUnitario": 5000.00, "subtotal": 10000.00 }
      ]
    }
  - Errores: 404 si la orden no existe

- PUT /api/orders/{id}/status (requiere rol ADMINISTRADOR o AUXILIAR)
  - Descripción: Actualiza el estado postpago (fulfillment) y registra un evento de seguimiento.
  - Body:
    {
      "estado": "PREPARING|SHIPPED|DELIVERED|READY_FOR_PICKUP", // compat
      "comentario": "Iniciando preparación",
      "trackingCode": "TRK-001",
      "fechaEntregaEstimada": "2025-11-15T09:00:00"
    }
  - Respuesta (200):
    {
      "orderId": 1,
      "status": "PREPARING",            // compat
      "paymentStatus": "PAID",
      "deliveryStatus": "PREPARING",
      "trackingCode": "TRK-001",
      "fechaEntregaEstimada": "2025-11-15T09:00:00",
      "timeline": [
        { "estado": "PREPARING", "comentario": "Iniciando preparación", "timestamp": "2025-11-12T11:00:00", "trackingCode": "TRK-001", "fechaEntregaEstimada": "2025-11-15T09:00:00" }
      ]
    }
  - Errores: 400 (estado inválido), 403 (no autorizado), 404 (orden no existe), 409 (transición no permitida)

- GET /api/orders/{id}/tracking (requiere dueño de la orden o staff)
  - Descripción: Retorna el timeline de seguimiento.
  - Respuesta (200):
    {
      "orderId": 1,
      "status": "PREPARING",
      "paymentStatus": "PAID",
      "deliveryStatus": "PREPARING",
      "trackingCode": "TRK-001",
      "fechaEntregaEstimada": "2025-11-15T09:00:00",
      "timeline": [
        { "estado": "PREPARING", "comentario": "Iniciando preparación", "timestamp": "2025-11-12T11:00:00", "trackingCode": "TRK-001", "fechaEntregaEstimada": "2025-11-15T09:00:00" }
      ]
    }
  - Errores: 401 (no autenticado), 403 (no autorizado), 404 (no existe)

- POST /api/orders/webhook
  - Descripción: Webhook de Mercado Pago. Procesa pagos y actualiza paymentStatus/stock. Devuelve 200 siempre.

---

## Órdenes por usuario

- GET /api/user/orders?idUsuario=ID
  - Descripción: Lista órdenes de un usuario.
  - Respuesta (200):
    [
      {
        "id": 1,
        "externalReference": "REF-123",
        "status": "PAID", // compat: es el paymentStatus
        "paymentId": "pay_123",
        "total": 35000.00,
        "fechaCreacion": "2025-11-12T10:20:30",
        "payerEmail": "cliente@correo.com",
        "items": [
          { "productId": 1, "nombre": "Pan integral", "cantidad": 2, "precioUnitario": 5000.00, "subtotal": 10000.00 }
        ]
      }
    ]

---

## Repartidores (Delivery)

- GET /api/user/deliveries/my-orders (requiere rol DELIVERY)
  - Descripción: Lista las órdenes asignadas al repartidor autenticado.
  - Respuesta (200):
    [
      {
        "orderId": 1,
        "externalReference": "REF-123",
        "paymentStatus": "PAID",
        "deliveryStatus": "PREPARING|DISPATCHED|DELIVERED",
        "trackingCode": "TRK-001",
        "fechaAsignacion": "2025-11-12T12:00:00",
        "total": 35000.00
      }
    ]

- PUT /api/user/deliveries/{orderId}/pickup (requiere rol DELIVERY y que la orden esté asignada al repartidor)
  - Descripción: Marca la orden como despachada (DISPATCHED). Úsalo en el momento de recogerla según la referencia mostrada (externalReference).
  - Body: vacío
  - Respuesta (200): { "message": "Orden marcada como despachada" }
  - Errores: 403 si no es el repartidor asignado o si la orden no está lista.

- PUT /api/user/deliveries/{orderId}/complete (requiere rol DELIVERY y orden asignada)
  - Descripción: Marca la orden como entregada (DELIVERED).
  - Body: vacío
  - Respuesta (200): { "message": "Orden marcada como entregada" }
  - Errores: 403 si no es el repartidor asignado o si la orden no está en despacho.

Notas:
- La asignación automática de órdenes se realiza en backend mediante el servicio `asignarOrdenesPendientesARepartidor(repartidorId, max)`. Si deseas exponerlo vía endpoint admin, agregaremos un endpoint de administración.
- deliveryStatus inicial tras PAID es CONFIRMED; cuando se asigna pasa a PREPARING automáticamente.

---

## Códigos de estado comunes
- 200 OK
- 201 Created (si aplica)
- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict
- 500 Internal Server Error

---

## Ejemplos de integración front

1) Flujo de compra:
- POST /api/orders/checkout → redirige a MP con initPoint
- Webhook MP → marca PAID + CONFIRMED
- Front hace polling:
  GET /api/orders/status?externalReference=REF
  - Si paymentStatus=PAID y deliveryStatus=CONFIRMED/PREPARING, mostrar “Pedido confirmado” y fechaEntregaEstimada/seguimiento.

2) Reparto:
- El back asigna órdenes al repartidor.
- El repartidor abre /api/user/deliveries/my-orders, ve externalReference.
- Pickup: PUT /api/user/deliveries/{orderId}/pickup
- Entregado: PUT /api/user/deliveries/{orderId}/complete

---

Última actualización: 2025-11-12

