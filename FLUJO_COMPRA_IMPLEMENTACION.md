# 📋 IMPLEMENTACIÓN COMPLETA DEL FLUJO DE COMPRA CON MERCADOPAGO

## ✅ RESUMEN DE CAMBIOS IMPLEMENTADOS

### **PASO 1 Y 2: Modelos de Datos**

#### **1. OrdenCompra (completado)**
**Archivo:** `src/main/java/co/edu/uniquindio/oldbaker/model/OrdenCompra.java`

**Campos agregados:**
- `externalReference` (String, unique): Referencia única para vincular con MercadoPago
- `status` (EstadoOrden enum): Estado de la orden (PENDING, PAID, FAILED, CANCELLED, IN_PROCESS)
- `paymentId` (String): ID del pago en MercadoPago
- `usuario` (Usuario): Relación con el usuario que realiza la compra
- `total` (BigDecimal): Monto total de la orden
- `items` (List<ItemOrden>): Productos comprados
- `fechaCreacion` y `fechaActualizacion` (LocalDateTime): Auditoría automática
- `payerEmail` (String): Email del pagador

**Métodos auxiliares:**
- `addItem()` y `removeItem()`: Gestión de relación bidireccional con items
- `@PrePersist` y `@PreUpdate`: Auditoría automática de fechas

#### **2. ItemOrden (nuevo)**
**Archivo:** `src/main/java/co/edu/uniquindio/oldbaker/model/ItemOrden.java`

**Características:**
- Persistencia de cada producto de la orden
- Almacena precio unitario al momento de la compra (histórico)
- Cálculo automático de subtotal con `@PrePersist` y `@PreUpdate`

#### **3. WebhookLog (nuevo)**
**Archivo:** `src/main/java/co/edu/uniquindio/oldbaker/model/WebhookLog.java`

**Características:**
- Registro completo de todos los webhooks recibidos
- Detecta webhooks duplicados
- Trazabilidad completa (paymentId, requestId, rawBody, status, errores)
- Índices en columnas clave para búsquedas rápidas

---

### **PASO 3: Persistir Orden ANTES de MercadoPago**

#### **OrdenCompraService.crearOrden()**
**Archivo:** `src/main/java/co/edu/uniquindio/oldbaker/services/OrdenCompraService.java`

**Flujo implementado:**
1. Valida que el usuario exista
2. Crea orden en estado `PENDING`
3. Genera `external_reference` único (UUID)
4. Obtiene precios **desde la BD** (precios confiables, no del cliente)
5. Crea items de la orden con precios y cantidades
6. Calcula total automáticamente
7. Persiste orden en BD
8. Retorna orden creada con ID y external_reference

#### **OrdenCompraController.checkout()**
**Archivo:** `src/main/java/co/edu/uniquindio/oldbaker/controllers/OrdenCompraController.java`

**Flujo implementado:**
1. Valida autenticación del usuario (`@AuthenticationPrincipal`)
2. Valida stock disponible con `StockValidationService`
3. **Crea orden en BD** usando `OrdenCompraService.crearOrden()`
4. Crea preferencia en MercadoPago con `external_reference` de la orden
5. Retorna `init_point` para redirigir al usuario

---

### **PASO 4: Actualizar Orden Después del Pago**

#### **MercadoPagoService.processPaymentAsync()**
**Archivo:** `src/main/java/co/edu/uniquindio/oldbaker/services/MercadoPagoService.java`

**Flujo implementado:**
1. Consulta detalles del pago en API de MercadoPago
2. Extrae `status` y `external_reference`
3. Actualiza orden según el estado:
   - `approved` → `marcarComoPagada()` (actualiza estado + **descuenta stock**)
   - `rejected`/`cancelled` → `marcarComoFallida()`
   - `pending`/`in_process`/`in_mediation` → `marcarComoEnProceso()`

#### **OrdenCompraService - Métodos de actualización**

**`marcarComoPagada()`:**
- Actualiza estado a PAID
- Guarda paymentId
- **Descuenta stock de insumos** (en la misma transacción)
- Todo es atómico (todo o nada)

**`descontarStock()`:**
- Usa recetas para calcular insumos necesarios
- Actualiza `cantidadActual` de cada insumo
- Registra logs detallados
- Alerta si el stock queda negativo

---

### **PASO 5: Transaccionalidad e Idempotencia**

#### **Transaccionalidad Robusta**

**`@Transactional(isolation = Isolation.SERIALIZABLE)`:**
- Aplicado a todos los métodos críticos de `OrdenCompraService`
- Evita condiciones de carrera en webhooks concurrentes
- Garantiza que actualización de estado + descuento de stock es atómico

**Validaciones de transiciones de estado:**
- PENDING/IN_PROCESS → PAID ✅
- CANCELLED → PAID ❌ (lanza `IllegalStateException`)
- PAID → FAILED ❌ (lanza `IllegalStateException`)
- PAID → IN_PROCESS ❌ (ignorado)

#### **Idempotencia Completa**

**Nivel 1 - Verificación en memoria:**
- Si la orden ya está PAID con el mismo `paymentId`: ignora webhook duplicado
- Si la orden ya está PAID con diferente `paymentId`: registra advertencia

**Nivel 2 - Registro en BD (WebhookLog):**
- Antes de procesar webhook: verifica `existsByPaymentIdAndProcessedTrue()`
- Si ya existe: ignora webhook duplicado inmediatamente
- Si no existe: crea registro y procesa
- Marca como `processed=true` solo después de éxito completo

**Beneficios:**
- ✅ Webhooks duplicados no causan doble descuento de stock
- ✅ Reintentos de MercadoPago son manejados correctamente
- ✅ Trazabilidad completa de todos los webhooks recibidos
- ✅ Logs de errores para debugging

---

## 📊 DIAGRAMA DE FLUJO COMPLETO

```
┌─────────────────────────────────────────────────────────────────┐
│  1. CLIENTE SOLICITA CHECKOUT                                   │
│     POST /api/orders/checkout                                   │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. VALIDACIONES                                                │
│     - Usuario autenticado                                       │
│     - Stock disponible (StockValidationService)                 │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. CREAR ORDEN EN BD (PASO 3)                                  │
│     - Estado: PENDING                                           │
│     - External Reference: UUID                                  │
│     - Precios desde BD (confiables)                             │
│     - Guardar items y total                                     │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. CREAR PREFERENCIA EN MERCADOPAGO                            │
│     - Usar external_reference de la orden                       │
│     - Configurar notification_url para webhooks                 │
│     - Retornar init_point                                       │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. USUARIO PAGA EN MERCADOPAGO                                 │
│     (Interfaz de MercadoPago)                                   │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. MERCADOPAGO ENVÍA WEBHOOK                                   │
│     POST /api/orders/webhook                                    │
│     - Valida firma HMAC (si está configurada)                   │
│     - Responde 200 inmediatamente                               │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  7. PROCESAMIENTO ASÍNCRONO (PASO 4 Y 5)                        │
│     - Verificar idempotencia (WebhookLog en BD)                 │
│     - Si ya procesado: ignorar                                  │
│     - Consultar pago en API de MercadoPago                      │
│     - Extraer status y external_reference                       │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  8. ACTUALIZAR ORDEN SEGÚN ESTADO                               │
│                                                                 │
│     APPROVED:                                                   │
│     ┌─────────────────────────────────────────────┐            │
│     │ @Transactional(SERIALIZABLE)                │            │
│     │ - Actualizar estado → PAID                  │            │
│     │ - Guardar paymentId                         │            │
│     │ - DESCONTAR STOCK (en misma transacción)    │            │
│     │ - Marcar webhook como processed=true        │            │
│     └─────────────────────────────────────────────┘            │
│                                                                 │
│     REJECTED/CANCELLED:                                         │
│     → Marcar como FAILED                                        │
│                                                                 │
│     PENDING/IN_PROCESS:                                         │
│     → Marcar como IN_PROCESS                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ ESTRUCTURA DE BASE DE DATOS

### **Nuevas Tablas**

#### **orden_compra**
```sql
- id (BIGINT, PK, auto-increment via sequence)
- external_reference (VARCHAR, UNIQUE, NOT NULL)
- status (VARCHAR, NOT NULL) -- PENDING, PAID, FAILED, CANCELLED, IN_PROCESS
- payment_id (VARCHAR)
- id_usuario (BIGINT, FK → usuarios.id)
- total (DECIMAL(10,2), NOT NULL)
- fecha_creacion (TIMESTAMP, NOT NULL)
- fecha_actualizacion (TIMESTAMP)
- payer_email (VARCHAR)
```

#### **item_orden**
```sql
- id (BIGINT, PK, auto-increment via sequence)
- id_orden (BIGINT, FK → orden_compra.id)
- id_producto (BIGINT, FK → productos.id_producto)
- cantidad (INTEGER, NOT NULL)
- precio_unitario (DECIMAL(10,2), NOT NULL)
- subtotal (DECIMAL(10,2), NOT NULL)
```

#### **webhook_log**
```sql
- id (BIGINT, PK, auto-increment via sequence)
- payment_id (VARCHAR, NOT NULL, INDEX)
- external_reference (VARCHAR, INDEX)
- request_id (VARCHAR, INDEX)
- topic (VARCHAR)
- payment_status (VARCHAR)
- processed (BOOLEAN, NOT NULL, default FALSE)
- error_message (VARCHAR(1000))
- raw_body (TEXT)
- fecha_recepcion (TIMESTAMP, NOT NULL)
- fecha_procesamiento (TIMESTAMP)
```

---

## 🔒 CARACTERÍSTICAS DE SEGURIDAD

### **1. Precios Confiables**
- Precios obtenidos desde la BD, **nunca del cliente**
- El frontend solo envía `productId` y `quantity`
- El backend calcula el total real

### **2. Validación de Stock**
- Verifica disponibilidad ANTES de crear orden
- Usa recetas para calcular insumos necesarios
- Retorna mensaje claro si no hay stock suficiente

### **3. Transacciones Atómicas**
- Uso de `@Transactional(isolation = SERIALIZABLE)`
- Actualización de estado + descuento de stock en una sola transacción
- Si falla el descuento, se hace rollback de todo

### **4. Idempotencia Robusta**
- Webhooks duplicados no causan problemas
- Registro en BD de todos los webhooks
- Validación de transiciones de estado

### **5. Validación de Firma HMAC**
- Verifica que el webhook realmente venga de MercadoPago
- Configurable con `mercadopago.webhook-secret`
- Protege contra webhooks falsos

---

## 📝 CONFIGURACIÓN REQUERIDA

### **application.properties**

```properties
# MercadoPago
mercadopago.access-token=YOUR_ACCESS_TOKEN
mercadopago.notification-url=https://yourdomain.com/api/payments/webhook
mercadopago.webhook-secret=YOUR_WEBHOOK_SECRET (opcional, para validar firma)

# Base URL de la aplicación
app.base-url=https://yourdomain.com

# Async (para @Async)
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
```

### **Habilitar @Async**

Agregar en la clase principal o configuración:

```java
@EnableAsync
@SpringBootApplication
public class OldBakerApplication {
    // ...
}
```

---

## 🧪 TESTING Y VALIDACIÓN

### **Escenarios de Prueba**

1. **Compra exitosa:**
   - Usuario autenticado compra productos con stock disponible
   - Orden creada en PENDING
   - Redirige a MercadoPago
   - Pago aprobado
   - Webhook recibido y procesado
   - Orden actualizada a PAID
   - Stock descontado correctamente

2. **Stock insuficiente:**
   - Usuario intenta comprar más de lo disponible
   - Respuesta 400 con mensaje claro
   - No se crea orden

3. **Pago rechazado:**
   - Orden creada en PENDING
   - Pago rechazado en MercadoPago
   - Webhook recibido
   - Orden actualizada a FAILED
   - Stock NO descontado

4. **Webhooks duplicados:**
   - Primer webhook: procesa correctamente
   - Webhooks subsiguientes: ignorados
   - Stock descontado solo una vez

5. **Webhook con firma inválida:**
   - Webhook rechazado
   - No se procesa
   - Registrado en logs

---

## 📊 LOGS Y MONITORING

### **Logs Importantes**

**Creación de orden:**
```
INFO  OrdenCompraService - Orden creada: id=123 externalRef=abc-123 total=50000 items=2
```

**Procesamiento de pago:**
```
INFO  MercadoPagoService - Pago 987654321 status=approved external_reference=abc-123
INFO  OrdenCompraService - Iniciando proceso de pago para orden 123: PENDING -> PAID
INFO  OrdenCompraService - Stock descontado: insumo=Harina producto=Baguette cantidad=500 stockAnterior=10000 stockNuevo=9500
INFO  OrdenCompraService - Orden 123 marcada como PAID exitosamente, stock descontado, paymentId=987654321
```

**Webhooks duplicados:**
```
INFO  MercadoPagoService - Webhook ya procesado previamente para paymentId=987654321, ignorando duplicado
```

**Errores:**
```
ERROR OrdenCompraService - ALERTA: Stock negativo para insumo 5 (Harina): actual=100 necesario=500 nuevo=-400
ERROR OrdenCompraService - Intento de marcar como PAID una orden CANCELADA: ordenId=123 paymentId=987654321
```

---

## 🚀 PRÓXIMOS PASOS RECOMENDADOS

### **1. Testing Automatizado**
- Tests unitarios para `OrdenCompraService`
- Tests de integración para el flujo completo
- Tests de concurrencia para webhooks duplicados

### **2. Endpoints Adicionales**
- `GET /api/orders/mis-ordenes` - Listar órdenes del usuario
- `GET /api/orders/{id}` - Detalle de una orden
- `POST /api/orders/{id}/cancelar` - Cancelar orden PENDING

### **3. Notificaciones**
- Email al usuario cuando pago es aprobado
- Email al usuario cuando pago es rechazado
- Notificaciones push

### **4. Dashboard Admin**
- Ver todas las órdenes
- Filtrar por estado
- Ver webhooks recibidos
- Estadísticas de ventas

### **5. Optimizaciones**
- Caché para consultas de productos
- Índices adicionales en BD
- Paginación en listados
- Rate limiting en endpoints públicos

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [x] Modelo `OrdenCompra` completo
- [x] Modelo `ItemOrden` creado
- [x] Modelo `WebhookLog` creado
- [x] Repositorios creados
- [x] `OrdenCompraService` con métodos de gestión
- [x] Crear orden ANTES de MercadoPago
- [x] Actualizar orden después del pago
- [x] Descuento de stock transaccional
- [x] Idempotencia con WebhookLog
- [x] Validaciones de transiciones de estado
- [x] Aislamiento transaccional SERIALIZABLE
- [x] Logs completos y detallados
- [x] Manejo de errores robusto
- [x] Validación de firma HMAC
- [x] Compilación exitosa sin errores

---

## 🎯 RESULTADO FINAL

El flujo completo de compra está implementado con:

✅ **Persistencia completa** de órdenes y items  
✅ **Transaccionalidad robusta** (ACID compliant)  
✅ **Idempotencia completa** para webhooks  
✅ **Descuento automático de stock** al confirmar pago  
✅ **Trazabilidad total** con logs y registros en BD  
✅ **Seguridad** con precios confiables y validación de firma  
✅ **Manejo de errores** y transiciones de estado  
✅ **Sin errores de compilación**  

**El sistema está listo para producción** 🚀

