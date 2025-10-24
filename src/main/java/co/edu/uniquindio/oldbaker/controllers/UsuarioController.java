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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://old-baker-front.vercel.app", "https://localhost:4200", "http://localhost:4200", "https://www.oldbaker.shop"})
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final OrdenCompraService ordenCompraService;

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
                dto.setStatus(orden.getStatus() != null ? orden.getStatus().name() : null);
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
}
