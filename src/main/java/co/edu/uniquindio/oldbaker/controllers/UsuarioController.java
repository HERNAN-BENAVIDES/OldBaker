package co.edu.uniquindio.oldbaker.controllers;

import co.edu.uniquindio.oldbaker.dto.DireccionResponseDTO;
import co.edu.uniquindio.oldbaker.dto.api.ApiResponse;
import co.edu.uniquindio.oldbaker.dto.auth.LogoutRequest;
import co.edu.uniquindio.oldbaker.services.AuthService;
import co.edu.uniquindio.oldbaker.services.UsuarioService;
import co.edu.uniquindio.oldbaker.services.OrdenCompraService;
import co.edu.uniquindio.oldbaker.model.OrdenCompra;
import co.edu.uniquindio.oldbaker.dto.order.OrdenCompraDTO;
import co.edu.uniquindio.oldbaker.dto.order.ItemOrdenDTO;
import co.edu.uniquindio.oldbaker.dto.cart.CartDTO;
import co.edu.uniquindio.oldbaker.services.CartService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import co.edu.uniquindio.oldbaker.model.Usuario;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://old-baker-front.vercel.app", "https://localhost:4200", "http://localhost:4200", "https://www.oldbaker.shop"})
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final OrdenCompraService ordenCompraService;
    private final CartService cartService;

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable("id") Long id) {
        boolean ok = usuarioService.deactivateUser(id);
        if (ok) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener el carrito del usuario
     */
    @GetMapping("/{idUsuario}/cart")
    public ResponseEntity<?> obtenerCart(@PathVariable Long idUsuario) {
        try {
            CartDTO cart = cartService.obtenerCart(idUsuario);
            if (cart == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            logger.warn("Error obteniendo cart para usuario {}: {}", idUsuario, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo obtener el carrito"));
        }
    }

    /**
     * Reemplaza los items del carrito del usuario con los enviados
     */
    @PutMapping("/{idUsuario}/cart")
    public ResponseEntity<?> actualizarCart(@PathVariable Long idUsuario, @RequestBody CartDTO request) {
        try {
            CartDTO actualizado = cartService.actualizarCart(idUsuario, request);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            logger.warn("Error actualizando cart para usuario {}: {}", idUsuario, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo actualizar el carrito"));
        }
    }

    /**
     * Obtener órdenes de compra de un usuario por su id.
     * Ejemplo: GET /api/user/orders?idUsuario=123
     */
    @GetMapping("/orders")
    public ResponseEntity<?> obtenerOrdenesPorUsuario(@RequestParam Long idUsuario) {
        try {
            List<OrdenCompra> ordenes = ordenCompraService.listarOrdenesPorUsuario(idUsuario);

            List<OrdenCompraDTO> dtoList = ordenes.stream().map(orden -> {
                List<ItemOrdenDTO> items = orden.getItems().stream().map(item -> new ItemOrdenDTO(
                        item.getProducto() != null ? item.getProducto().getIdProducto() : null,
                        item.getProducto() != null ? item.getProducto().getNombre() : null,
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal()
                )).collect(Collectors.toList());

                OrdenCompraDTO dto = new OrdenCompraDTO();
                dto.setId(orden.getId());
                dto.setExternalReference(orden.getExternalReference());
                // compatibilidad: mantener "status" como paymentStatus
                dto.setStatus(orden.getPaymentStatus() != null ? orden.getPaymentStatus().name() : null);
                // nuevos campos explícitos
                dto.setPaymentStatus(orden.getPaymentStatus() != null ? orden.getPaymentStatus().name() : null);
                dto.setDeliveryStatus(orden.getDeliveryStatus() != null ? orden.getDeliveryStatus().name() : null);
                dto.setPaymentId(orden.getPaymentId());
                dto.setTotal(orden.getTotal());
                dto.setFechaCreacion(orden.getFechaCreacion());
                dto.setPayerEmail(orden.getPayerEmail());
                dto.setItems(items);
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            logger.warn("Error obteniendo órdenes para usuario {}: {}", idUsuario, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "No se pudieron obtener las órdenes"));
        }
    }

    /**
     * Endpoint para cerrar la sesión de un usuario.
     *
     * @param request Datos necesarios para cerrar la sesión.
     * @return Respuesta con el resultado del cierre de sesión.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {

        try {
            // Llamar al servicio para cerrar la sesión
            String response = authService.logout(request);
            return ResponseEntity.ok(ApiResponse.success("Logout exitoso", response));
        }catch (IllegalArgumentException e) {
            return  ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("direccion")
    public ResponseEntity<List<DireccionResponseDTO>> obtenerDireccionUsuario(@RequestParam Long idUsuario) {
        return ResponseEntity.ok(usuarioService.obtenerDireccionUsuario(idUsuario));
    }

    @PostMapping("/agregar-direccion")
    public ResponseEntity<DireccionResponseDTO> agregarDireccionUsuario(@RequestParam Long idUsuario, @RequestBody DireccionResponseDTO direccionDTO) {
        DireccionResponseDTO direccionAgregada = usuarioService.agregarDireccionUsuario(idUsuario, direccionDTO);
        return ResponseEntity.ok(direccionAgregada);
    }


    @GetMapping("/deliveries/my-orders")
    public ResponseEntity<?> listarOrdenesRepartidor(@AuthenticationPrincipal Usuario repartidor) {
        if (repartidor == null || repartidor.getRol() != Usuario.Rol.DELIVERY) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }
        List<OrdenCompra> ordenes = ordenCompraService.listarOrdenesDeRepartidor(repartidor.getId());
        return ResponseEntity.ok(ordenes.stream().map(o -> {
            Map<String, Object> ordenMap = new HashMap<>();
            ordenMap.put("orderId", o.getId());
            ordenMap.put("externalReference", o.getExternalReference());
            ordenMap.put("paymentStatus", o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null);
            ordenMap.put("deliveryStatus", o.getDeliveryStatus() != null ? o.getDeliveryStatus().name() : null);
            ordenMap.put("trackingCode", o.getTrackingCode());
            ordenMap.put("fechaAsignacion", o.getFechaAsignacionRepartidor());
            ordenMap.put("total", o.getTotal());

            if (o.getDireccion() != null) {
                Map<String, String> direccionMap = new HashMap<>();
                direccionMap.put("ciudad", o.getDireccion().getCiudad());
                direccionMap.put("barrio", o.getDireccion().getBarrio());
                direccionMap.put("carrera", o.getDireccion().getCarrera());
                direccionMap.put("calle", o.getDireccion().getCalle());
                direccionMap.put("numero", o.getDireccion().getNumero());
                direccionMap.put("numeroTelefono", o.getDireccion().getNumeroTelefono());
                ordenMap.put("direccion", direccionMap);
            } else {
                ordenMap.put("direccion", null);
            }

            return ordenMap;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/deliveries/{orderId}/pickup")
    public ResponseEntity<?> pickupOrden(@PathVariable Long orderId, @AuthenticationPrincipal Usuario repartidor) {
        try {
            ordenCompraService.marcarRecolectadaPorRepartidor(orderId, repartidor);
            return ResponseEntity.ok(Map.of("message", "Orden marcada como despachada"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/deliveries/{orderId}/complete")
    public ResponseEntity<?> completarEntrega(@PathVariable Long orderId, @AuthenticationPrincipal Usuario repartidor) {
        try {
            ordenCompraService.marcarEntregadaPorRepartidor(orderId, repartidor);
            return ResponseEntity.ok(Map.of("message", "Orden marcada como entregada"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
