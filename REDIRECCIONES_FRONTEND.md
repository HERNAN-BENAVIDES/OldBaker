# 🔄 REDIRECCIONES DE MERCADOPAGO AL FRONTEND

## 📋 CONFIGURACIÓN IMPLEMENTADA

### **1. URLs de Redirección (back_urls)**

He configurado el sistema para que MercadoPago redirija automáticamente al usuario a tu frontend después del pago. Las URLs de redirección se configuran en la preferencia y dependen del resultado del pago:

- **✅ success**: Pago aprobado
- **❌ failure**: Pago rechazado
- **⏳ pending**: Pago pendiente

### **2. Configuración en application.properties**

Agrega esta variable de entorno:

```properties
# URL del frontend (donde MercadoPago redirigirá al usuario)
app.frontend-url=${FRONTEND_URL:https://old-baker-front.vercel.app}
```

**Ejemplos según ambiente:**

```bash
# Desarrollo local
FRONTEND_URL=http://localhost:4200

# Producción
FRONTEND_URL=https://old-baker-front.vercel.app
```

---

## 🔗 URLs DE REDIRECCIÓN GENERADAS

Cuando creas una preferencia, el sistema automáticamente configura estas URLs:

```
Success: https://old-baker-front.vercel.app/payment/success?external_reference=abc-123
Failure: https://old-baker-front.vercel.app/payment/failure?external_reference=abc-123
Pending: https://old-baker-front.vercel.app/payment/pending?external_reference=abc-123
```

### **Parámetros en la URL:**

MercadoPago agrega automáticamente varios parámetros:

- `external_reference`: Tu referencia de la orden (UUID)
- `collection_id`: ID del pago en MercadoPago (payment_id)
- `collection_status`: Estado del pago (approved, rejected, pending, etc.)
- `payment_id`: Alias de collection_id
- `status`: Estado general
- `preference_id`: ID de la preferencia
- `merchant_order_id`: ID de la orden de MercadoPago

**Ejemplo de URL completa que recibirá tu frontend:**

```
https://old-baker-front.vercel.app/payment/success?
  external_reference=abc-123-456&
  collection_id=1234567890&
  collection_status=approved&
  payment_id=1234567890&
  status=approved&
  preference_id=123456-abc&
  merchant_order_id=987654
```

---

## 🎨 IMPLEMENTACIÓN EN EL FRONTEND (Angular)

### **1. Crear las rutas en el routing**

```typescript
// app-routing.module.ts
import { PaymentSuccessComponent } from './payment/payment-success.component';
import { PaymentFailureComponent } from './payment/payment-failure.component';
import { PaymentPendingComponent } from './payment/payment-pending.component';

const routes: Routes = [
  // ...otras rutas...
  { path: 'payment/success', component: PaymentSuccessComponent },
  { path: 'payment/failure', component: PaymentFailureComponent },
  { path: 'payment/pending', component: PaymentPendingComponent },
];
```

### **2. Componente de Pago Exitoso (payment-success)**

```typescript
// payment-success.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../services/order.service';

@Component({
  selector: 'app-payment-success',
  templateUrl: './payment-success.component.html'
})
export class PaymentSuccessComponent implements OnInit {
  order: any;
  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    // Obtener external_reference de los query params
    this.route.queryParams.subscribe(params => {
      const externalReference = params['external_reference'];
      const paymentId = params['payment_id'] || params['collection_id'];
      
      if (externalReference) {
        this.loadOrderStatus(externalReference);
      } else {
        this.error = 'No se encontró la referencia de la orden';
        this.loading = false;
      }
    });
  }

  loadOrderStatus(externalReference: string): void {
    this.orderService.getOrderStatus(externalReference).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
        console.log('Orden cargada:', order);
      },
      error: (err) => {
        this.error = 'Error al cargar la orden';
        this.loading = false;
        console.error('Error:', err);
      }
    });
  }
}
```

```html
<!-- payment-success.component.html -->
<div class="container mt-5">
  <div *ngIf="loading" class="text-center">
    <div class="spinner-border text-success" role="status">
      <span class="visually-hidden">Cargando...</span>
    </div>
    <p class="mt-3">Verificando tu pago...</p>
  </div>

  <div *ngIf="!loading && order" class="alert alert-success">
    <h1 class="display-4">✅ ¡Pago Exitoso!</h1>
    <hr>
    <p class="lead">Tu pedido ha sido confirmado y procesado correctamente.</p>
    
    <div class="mt-4">
      <h4>Detalles de la Orden</h4>
      <p><strong>Orden ID:</strong> {{ order.orderId }}</p>
      <p><strong>Estado:</strong> {{ order.status }}</p>
      <p><strong>Total:</strong> ${{ order.total | number:'1.2-2' }}</p>
      <p><strong>Payment ID:</strong> {{ order.paymentId }}</p>
      
      <h5 class="mt-3">Productos:</h5>
      <ul>
        <li *ngFor="let item of order.items">
          {{ item.producto }} - Cantidad: {{ item.cantidad }} - 
          ${{ item.subtotal | number:'1.2-2' }}
        </li>
      </ul>
    </div>

    <div class="mt-4">
      <button class="btn btn-primary" routerLink="/mis-pedidos">
        Ver mis pedidos
      </button>
      <button class="btn btn-outline-secondary ms-2" routerLink="/">
        Volver al inicio
      </button>
    </div>
  </div>

  <div *ngIf="!loading && error" class="alert alert-danger">
    <h4>Error</h4>
    <p>{{ error }}</p>
    <button class="btn btn-primary" routerLink="/">Volver al inicio</button>
  </div>
</div>
```

### **3. Componente de Pago Rechazado (payment-failure)**

```typescript
// payment-failure.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-payment-failure',
  templateUrl: './payment-failure.component.html'
})
export class PaymentFailureComponent implements OnInit {
  externalReference: string | null = null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.externalReference = params['external_reference'];
    });
  }
}
```

```html
<!-- payment-failure.component.html -->
<div class="container mt-5">
  <div class="alert alert-danger">
    <h1 class="display-4">❌ Pago Rechazado</h1>
    <hr>
    <p class="lead">Lo sentimos, tu pago no pudo ser procesado.</p>
    
    <div class="mt-4">
      <h5>¿Qué puedes hacer?</h5>
      <ul>
        <li>Verificar que tu tarjeta tenga fondos suficientes</li>
        <li>Intentar con otro método de pago</li>
        <li>Contactar a tu banco</li>
      </ul>
    </div>

    <div class="mt-4">
      <button class="btn btn-primary" routerLink="/carrito">
        Intentar nuevamente
      </button>
      <button class="btn btn-outline-secondary ms-2" routerLink="/">
        Volver al inicio
      </button>
    </div>
  </div>
</div>
```

### **4. Componente de Pago Pendiente (payment-pending)**

```typescript
// payment-pending.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../services/order.service';

@Component({
  selector: 'app-payment-pending',
  templateUrl: './payment-pending.component.html'
})
export class PaymentPendingComponent implements OnInit {
  order: any;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const externalReference = params['external_reference'];
      if (externalReference) {
        this.loadOrderStatus(externalReference);
      }
    });
  }

  loadOrderStatus(externalReference: string): void {
    this.orderService.getOrderStatus(externalReference).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
}
```

```html
<!-- payment-pending.component.html -->
<div class="container mt-5">
  <div class="alert alert-warning">
    <h1 class="display-4">⏳ Pago Pendiente</h1>
    <hr>
    <p class="lead">Tu pago está siendo procesado.</p>
    
    <div class="mt-4">
      <p>Recibirás una notificación cuando se confirme el pago.</p>
      <p *ngIf="order">
        <strong>Orden ID:</strong> {{ order.orderId }}<br>
        <strong>Total:</strong> ${{ order.total | number:'1.2-2' }}
      </p>
    </div>

    <div class="mt-4">
      <button class="btn btn-primary" routerLink="/mis-pedidos">
        Ver mis pedidos
      </button>
      <button class="btn btn-outline-secondary ms-2" routerLink="/">
        Volver al inicio
      </button>
    </div>
  </div>
</div>
```

### **5. Servicio para consultar el estado de la orden**

```typescript
// services/order.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private apiUrl = environment.apiUrl + '/api/orders';

  constructor(private http: HttpClient) {}

  getOrderStatus(externalReference: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/status?externalReference=${externalReference}`);
  }

  getMyOrders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/mis-ordenes`);
  }
}
```

---

## 🔄 FLUJO COMPLETO

```
1. Usuario hace clic en "Pagar"
   ↓
2. Frontend llama a POST /api/orders/checkout
   ↓
3. Backend crea orden en BD (PENDING)
   ↓
4. Backend crea preferencia en MercadoPago con back_urls
   ↓
5. Backend retorna init_point
   ↓
6. Frontend redirige a init_point (página de pago de MercadoPago)
   ↓
7. Usuario paga en MercadoPago
   ↓
8. MercadoPago envía webhook al backend (procesa asíncronamente)
   ↓
9. MercadoPago redirige al usuario a:
   - /payment/success (si aprobado)
   - /payment/failure (si rechazado)
   - /payment/pending (si pendiente)
   ↓
10. Frontend extrae external_reference de query params
    ↓
11. Frontend llama a GET /api/orders/status?externalReference=xxx
    ↓
12. Backend retorna estado actualizado de la orden
    ↓
13. Frontend muestra resultado al usuario
```

---

## 🎯 ENDPOINT NUEVO IMPLEMENTADO

### **GET /api/orders/status**

**Query params:**
- `externalReference` (required): UUID de la orden

**Respuesta exitosa (200):**
```json
{
  "orderId": 123,
  "status": "PAID",
  "total": 50000.00,
  "paymentId": "1234567890",
  "fechaCreacion": "2025-10-17T10:30:00",
  "items": [
    {
      "producto": "Baguette",
      "cantidad": 2,
      "precioUnitario": 5000.00,
      "subtotal": 10000.00
    }
  ]
}
```

**Estados posibles:**
- `PENDING`: Orden creada, esperando pago
- `PAID`: Pago confirmado
- `FAILED`: Pago rechazado
- `IN_PROCESS`: Pago en proceso
- `CANCELLED`: Orden cancelada

---

## ⚙️ CONFIGURACIÓN FINAL

### **1. Variables de entorno (Backend)**

```bash
# .env o configuración de deployment
FRONTEND_URL=https://old-baker-front.vercel.app
```

### **2. Variables de entorno (Frontend Angular)**

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};

// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.oldbaker.shop'
};
```

---

## 🧪 PRUEBAS

### **Flujo de prueba completo:**

1. **Iniciar checkout:**
   ```bash
   POST https://api.oldbaker.shop/api/orders/checkout
   ```

2. **Redirigir a init_point** (abre en navegador)

3. **Usar tarjetas de prueba de MercadoPago:**
   - **Aprobado:** 5031 7557 3453 0604 (CVV: 123, Exp: cualquier fecha futura)
   - **Rechazado:** 5031 4332 1540 6351
   - **Pendiente:** 5031 4348 8415 5283

4. **Verificar redirección:**
   - URL: `https://old-baker-front.vercel.app/payment/success?external_reference=...`

5. **Verificar estado en backend:**
   ```bash
   GET https://api.oldbaker.shop/api/orders/status?externalReference=abc-123
   ```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

### Backend:
- [x] Configurar `app.frontend-url` en application.properties
- [x] back_urls incluyen external_reference en query param
- [x] Endpoint GET /api/orders/status creado
- [x] CORS configurado para el dominio del frontend

### Frontend:
- [ ] Crear componentes: PaymentSuccessComponent, PaymentFailureComponent, PaymentPendingComponent
- [ ] Crear OrderService con getOrderStatus()
- [ ] Configurar rutas en routing module
- [ ] Agregar estilos para las páginas de resultado
- [ ] Probar flujo completo con tarjetas de prueba

---

## 🚨 IMPORTANTE

1. **El webhook actualiza el estado asíncronamente:** El usuario puede llegar a la página de success ANTES de que el webhook procese el pago. Por eso es importante consultar el estado vía API.

2. **Polling opcional:** Si el estado aún es PENDING cuando el usuario llega, puedes implementar polling cada 2-3 segundos hasta que cambie a PAID.

3. **Timeout del webhook:** MercadoPago puede tardar varios segundos en enviar el webhook. No asumas que el pago está confirmado solo por la redirección.

4. **Manejo de errores:** Si el external_reference no se encuentra, mostrar mensaje amigable y botón para volver al inicio.

---

## 📞 SOPORTE

Si tienes problemas:
- Revisa los logs del backend para ver si llegó el webhook
- Verifica que las URLs de redirección sean correctas
- Prueba con las tarjetas de test de MercadoPago
- Consulta la documentación oficial: https://www.mercadopago.com.co/developers

