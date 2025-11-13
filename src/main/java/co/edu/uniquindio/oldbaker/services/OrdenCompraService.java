package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.order.OrderTrackingDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutItemDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutRequestDTO;
import co.edu.uniquindio.oldbaker.model.*;
import co.edu.uniquindio.oldbaker.repositories.DireccionRepository;
import co.edu.uniquindio.oldbaker.repositories.InsumoRepository;
import co.edu.uniquindio.oldbaker.repositories.OrdenCompraRepository;
import co.edu.uniquindio.oldbaker.repositories.ProductRepository;
import co.edu.uniquindio.oldbaker.repositories.RecetaRepository;
import co.edu.uniquindio.oldbaker.repositories.SeguimientoPedidoRepository;
import co.edu.uniquindio.oldbaker.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private static final Logger logger = LoggerFactory.getLogger(OrdenCompraService.class);

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProductRepository productRepository;
    private final UsuarioRepository usuarioRepository;
    private final InsumoRepository insumoRepository;
    private final RecetaRepository recetaRepository;
    private final SeguimientoPedidoRepository seguimientoPedidoRepository;
    private final DireccionRepository direccionRepository;

    private static final Map<OrdenCompra.DeliveryStatus, List<OrdenCompra.DeliveryStatus>> TRANSICIONES_ENTREGA = Map.of(
            OrdenCompra.DeliveryStatus.CONFIRMED, List.of(
                    OrdenCompra.DeliveryStatus.PREPARING,
                    OrdenCompra.DeliveryStatus.DISPATCHED,
                    OrdenCompra.DeliveryStatus.DELIVERED
            ),
            OrdenCompra.DeliveryStatus.PREPARING, List.of(
                    OrdenCompra.DeliveryStatus.DISPATCHED,
                    OrdenCompra.DeliveryStatus.DELIVERED
            ),
            OrdenCompra.DeliveryStatus.DISPATCHED, List.of(
                    OrdenCompra.DeliveryStatus.DELIVERED
            )
    );

    /**
     * Crea una orden en estado PENDING antes de redirigir a MercadoPago.
     * Calcula el total desde la BD (precios confiables) y genera external_reference único.
     */
    @Transactional
    public OrdenCompra crearOrden(CheckoutRequestDTO request, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioId));

        Direccion direccion = null;
        if (request.getDireccionId() != null) {
            direccion = direccionRepository.findById(request.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada: " + request.getDireccionId()));
            // Opcional: verificar que la dirección pertenece al usuario
            if (!direccion.getUsuario().getId().equals(usuarioId)) {
                throw new AccessDeniedException("La dirección no pertenece al usuario");
            }
        }

        OrdenCompra orden = OrdenCompra.builder()
                .externalReference(UUID.randomUUID().toString())
                .paymentStatus(OrdenCompra.PaymentStatus.PENDING)
                .usuario(usuario)
                .direccion(direccion)
                .fechaEntregaEstimada(request.getFechadeEntregaEstimada())
                .payerEmail(request.getPayerEmail())
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal totalCalculado = BigDecimal.ZERO;

        for (CheckoutItemDTO itemDTO : request.getItems()) {
            Producto producto = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemDTO.getProductId()));

            BigDecimal precioUnitario = BigDecimal.valueOf(producto.getCostoUnitario());
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalCalculado = totalCalculado.add(subtotal);

            ItemOrden item = ItemOrden.builder()
                    .producto(producto)
                    .cantidad(itemDTO.getQuantity())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .build();

            orden.addItem(item);
        }

        orden.setTotal(totalCalculado);
        OrdenCompra ordenGuardada = ordenCompraRepository.save(orden);

        logger.info("Orden creada: id={} externalRef={} total={} items={}",
                ordenGuardada.getId(), ordenGuardada.getExternalReference(),
                ordenGuardada.getTotal(), ordenGuardada.getItems().size());

        return ordenGuardada;
    }

    /**
     * Paso 5: Transaccionalidad completa e idempotencia.
     * Marca una orden como PAID y descuenta el stock de insumos EN LA MISMA TRANSACCIÓN.
     *
     * Características de idempotencia:
     * - Si la orden ya está PAID con el mismo paymentId: no hace nada (webhook duplicado exacto)
     * - Si la orden ya está PAID con diferente paymentId: registra advertencia pero no falla
     * - Usa SERIALIZABLE para evitar condiciones de carrera en webhooks concurrentes
     * - Todo el proceso (actualizar estado + descontar stock) es atómico
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void marcarComoPagada(String externalReference, String paymentId) {
        OrdenCompra orden = ordenCompraRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));

        // Idempotencia: verificar si ya está procesada
        if (orden.getPaymentStatus() == OrdenCompra.PaymentStatus.PAID) {
            if (paymentId.equals(orden.getPaymentId())) {
                logger.info("Orden {} ya está PAID con paymentId={} (idempotente)", orden.getId(), paymentId);
            } else {
                logger.warn("Orden {} ya PAID con otro paymentId. Anterior: {}, Nuevo: {}",
                        orden.getId(), orden.getPaymentId(), paymentId);
            }
            return;
        }

        // Verificar transiciones de estado válidas
        if (orden.getPaymentStatus() == OrdenCompra.PaymentStatus.CANCELLED) {
            logger.error("Intento de marcar como PAID una orden CANCELLED: ordenId={} paymentId={}", orden.getId(), paymentId);
            throw new IllegalStateException("No se puede marcar como PAID una orden cancelada");
        }

        logger.info("Pago confirmado para orden {}: {} -> PAID", orden.getId(), orden.getPaymentStatus());

        orden.setPaymentStatus(OrdenCompra.PaymentStatus.PAID);
        orden.setPaymentId(paymentId);

        // Primera transición de fulfillment al confirmarse pago
        if (orden.getDeliveryStatus() == null) {
            orden.setDeliveryStatus(OrdenCompra.DeliveryStatus.CONFIRMED);
            seguimientoPedidoRepository.save(
                    SeguimientoPedido.builder()
                            .orden(orden)
                            .estado(OrdenCompra.DeliveryStatus.CONFIRMED)
                            .comentario("Pedido confirmado para producción/entrega futura")
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }

        // Asignación automática de repartidor si no existe
        if (orden.getRepartidor() == null) {
            List<Usuario> repartidores = usuarioRepository.findByRolAndActivoTrue(Usuario.Rol.DELIVERY);
            if (!repartidores.isEmpty()) {
                // Round-robin simple: usar hash de externalReference para distribuir
                int idx = Math.floorMod(orden.getExternalReference().hashCode(), repartidores.size());
                Usuario elegido = repartidores.get(idx);
                orden.setRepartidor(elegido);
                orden.setFechaAsignacionRepartidor(LocalDateTime.now());
                // Al asignar, si está en CONFIRMED lo movemos a PREPARING
                if (orden.getDeliveryStatus() == OrdenCompra.DeliveryStatus.CONFIRMED) {
                    orden.setDeliveryStatus(OrdenCompra.DeliveryStatus.PREPARING);
                }
            } else {
                logger.warn("No hay repartidores activos para asignar orden {}", orden.getId());
            }
        }

        // Generar trackingCode si no existe
        if (orden.getTrackingCode() == null || orden.getTrackingCode().isBlank()) {
            String shortRef = orden.getExternalReference().substring(0, Math.min(8, orden.getExternalReference().length())).toUpperCase();
            String tracking = "OB-" + shortRef + "-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMddHHmm"));
            orden.setTrackingCode(tracking);
        }

        // Descontar stock de insumos (en la misma transacción)
        descontarStock(orden);

        ordenCompraRepository.save(orden);
        logger.info("Orden {} marcada como PAID; asignación={}, tracking={}",
                orden.getId(), orden.getRepartidor() != null ? orden.getRepartidor().getId() : null, orden.getTrackingCode());
    }

    /**
     * Marca una orden como FAILED (pago rechazado).
     * También es idempotente para manejar webhooks duplicados.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void marcarComoFallida(String externalReference, String paymentId) {
        OrdenCompra orden = ordenCompraRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));

        // Idempotencia: si ya está FAILED con el mismo paymentId, ignorar
        if (orden.getPaymentStatus() == OrdenCompra.PaymentStatus.FAILED) {
            if (paymentId != null && paymentId.equals(orden.getPaymentId())) {
                logger.info("Orden {} ya está marcada como FAILED con el mismo paymentId={}, ignorando",
                        orden.getId(), paymentId);
                return;
            }
        }

        // Si ya está PAID, no permitir cambio a FAILED
        if (orden.getPaymentStatus() == OrdenCompra.PaymentStatus.PAID) {
            logger.error("Intento de marcar como FAILED una orden ya pagada: {}", orden.getId());
            throw new IllegalStateException("No se puede marcar como FAILED una orden ya pagada");
        }

        orden.setPaymentStatus(OrdenCompra.PaymentStatus.FAILED);
        orden.setPaymentId(paymentId);
        ordenCompraRepository.save(orden);

        logger.info("Orden {} marcada como FAILED", orden.getId());
    }

    /**
     * Marca una orden como IN_PROCESS (pago pendiente).
     * También es idempotente.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void marcarComoEnProceso(String externalReference, String paymentId) {
        OrdenCompra orden = ordenCompraRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));

        // Si ya está PAID, no cambiar a IN_PROCESS (pago confirmado tiene prioridad)
        if (orden.getPaymentStatus() == OrdenCompra.PaymentStatus.PAID) {
            logger.info("Orden {} ya está PAID, ignorando IN_PROCESS", orden.getId());
            return;
        }

        // Idempotencia: si ya está IN_PROCESS con el mismo paymentId, ignorar
        if (orden.getPaymentStatus() == OrdenCompra.PaymentStatus.IN_PROCESS &&
                Objects.equals(paymentId, orden.getPaymentId())) {
            logger.info("Orden {} ya está IN_PROCESS (idempotente)", orden.getId());
            return;
        }

        orden.setPaymentStatus(OrdenCompra.PaymentStatus.IN_PROCESS);
        orden.setPaymentId(paymentId);
        ordenCompraRepository.save(orden);
        logger.info("Orden {} marcada como IN_PROCESS", orden.getId());
    }

    @Transactional
    public List<OrderTrackingDTO> registrarCambioEstadoEntrega(Long ordenId,
                                                                OrdenCompra.DeliveryStatus nuevoEstado,
                                                                String comentario,
                                                                String trackingCode,
                                                                LocalDateTime fechaEntregaEstimada) {
        OrdenCompra orden = obtenerOrdenPorIdInterno(ordenId);

        if (orden.getPaymentStatus() != OrdenCompra.PaymentStatus.PAID) {
            throw new IllegalStateException("No se puede actualizar entrega si el pago no está confirmado");
        }

        validarTransicionEntrega(orden, nuevoEstado);

        if (trackingCode != null && !trackingCode.isBlank()) {
            orden.setTrackingCode(trackingCode.trim());
        }
        if (fechaEntregaEstimada != null) {
            orden.setFechaEntregaEstimada(fechaEntregaEstimada);
        }
        if (orden.getDeliveryStatus() != nuevoEstado) {
            orden.setDeliveryStatus(nuevoEstado);
        }

        ordenCompraRepository.save(orden);

        seguimientoPedidoRepository.save(
                SeguimientoPedido.builder()
                        .orden(orden)
                        .estado(nuevoEstado)
                        .comentario(comentario)
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        return construirTimelineSeguimiento(orden);
    }

    @Transactional(readOnly = true)
    public List<OrderTrackingDTO> obtenerSeguimientoOrden(Long ordenId) {
        OrdenCompra orden = obtenerOrdenPorIdInterno(ordenId);
        return construirTimelineSeguimiento(orden);
    }

    @Transactional(readOnly = true)
    public OrdenCompra obtenerPorId(Long ordenId) {
        OrdenCompra orden = obtenerOrdenPorIdInterno(ordenId);
        if (orden.getUsuario() != null) {
            orden.getUsuario().getId();
        }
        return orden;
    }

    /**
     * Descuenta el stock de insumos necesarios para preparar los productos de la orden.
     * Utiliza las recetas para calcular las cantidades exactas.
     *
     * IMPORTANTE: Este método se ejecuta dentro de la transacción de marcarComoPagada(),
     * garantizando atomicidad (todo o nada).
     */
    private void descontarStock(OrdenCompra orden) {
        for (ItemOrden item : orden.getItems()) {
            Long productoId = item.getProducto().getIdProducto();
            int cantidad = item.getCantidad();

            List<Receta> recetas = recetaRepository.findByProducto_IdProducto(productoId);

            if (recetas.isEmpty()) {
                logger.warn("Producto {} no tiene recetas, no se descuenta stock", productoId);
                continue;
            }

            for (Receta receta : recetas) {
                Insumo insumo = receta.getInsumo();
                double cantidadPorProducto = receta.getCantidadInsumo();
                double totalNecesario = cantidadPorProducto * cantidad;

                int stockActual = insumo.getCantidadActual() != null ? insumo.getCantidadActual() : 0;
                int nuevoStock = (int) Math.round(stockActual - totalNecesario);

                if (nuevoStock < 0) {
                    logger.error("ALERTA: Stock negativo para insumo {} ({}): actual={} necesario={} nuevo={}",
                            insumo.getIdInsumo(), insumo.getNombre(), stockActual, totalNecesario, nuevoStock);
                }

                insumo.setCantidadActual(nuevoStock);
                insumoRepository.save(insumo);

                logger.info("Stock descontado: insumo={} producto={} cantidad={} stockAnterior={} stockNuevo={}",
                        insumo.getNombre(), item.getProducto().getNombre(), totalNecesario, stockActual, nuevoStock);
            }
        }
    }

    /**
     * Obtiene una orden por su external_reference.
     */
    public OrdenCompra obtenerPorExternalReference(String externalReference) {
        return ordenCompraRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));
    }

    /**
     * Lista todas las órdenes de un usuario.
     */
    public List<OrdenCompra> listarOrdenesPorUsuario(Long usuarioId) {
        return ordenCompraRepository.findByUsuario_IdOrderByFechaCreacionDesc(usuarioId);
    }

    private void validarTransicionEntrega(OrdenCompra orden, OrdenCompra.DeliveryStatus nuevoEstado) {
        OrdenCompra.DeliveryStatus estadoActual = orden.getDeliveryStatus();

        if (orden.getPaymentStatus() != OrdenCompra.PaymentStatus.PAID) {
            throw new IllegalStateException("Transición de entrega no permitida: pago no confirmado");
        }

        if (estadoActual == OrdenCompra.DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("La orden ya fue entregada");
        }

        if (estadoActual == null) {
            // Primera transición válida desde CONFIRMED
            if (nuevoEstado == OrdenCompra.DeliveryStatus.CONFIRMED ||
                nuevoEstado == OrdenCompra.DeliveryStatus.PREPARING ||
                nuevoEstado == OrdenCompra.DeliveryStatus.DISPATCHED ||
                nuevoEstado == OrdenCompra.DeliveryStatus.DELIVERED) {
                return;
            }
        }

        if (estadoActual == nuevoEstado) {
            return;
        }

        List<OrdenCompra.DeliveryStatus> permitidos = TRANSICIONES_ENTREGA.getOrDefault(estadoActual, List.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new IllegalStateException("Transición de entrega de " + estadoActual + " a " + nuevoEstado + " no permitida");
        }
    }

    private OrdenCompra obtenerOrdenPorIdInterno(Long ordenId) {
        return ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + ordenId));
    }

    private List<OrderTrackingDTO> construirTimelineSeguimiento(OrdenCompra orden) {
        return seguimientoPedidoRepository.findByOrden_IdOrderByTimestampAsc(orden.getId())
                .stream()
                .map(evento -> OrderTrackingDTO.builder()
                        .estado(evento.getEstado())
                        .comentario(evento.getComentario())
                        .timestamp(evento.getTimestamp())
                        .trackingCode(orden.getTrackingCode())
                        .fechaEntregaEstimada(orden.getFechaEntregaEstimada())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public List<OrdenCompra> asignarOrdenesPendientesARepartidor(Long repartidorId, int maxAsignaciones) {
        List<OrdenCompra> pendientes = ordenCompraRepository.findOrdenesPendientesDeAsignar();
        List<OrdenCompra> asignadas = new ArrayList<>();
        int count = 0;
        for (OrdenCompra o : pendientes) {
            if (count >= maxAsignaciones) break;
            o.setRepartidor(usuarioRepository.findById(repartidorId)
                    .orElseThrow(() -> new RuntimeException("Repartidor no encontrado: " + repartidorId)));
            o.setFechaAsignacionRepartidor(LocalDateTime.now());
            if (o.getDeliveryStatus() == OrdenCompra.DeliveryStatus.CONFIRMED) {
                o.setDeliveryStatus(OrdenCompra.DeliveryStatus.PREPARING);
            }
            ordenCompraRepository.save(o);
            asignadas.add(o);
            count++;
        }
        return asignadas;
    }

    @Transactional(readOnly = true)
    public List<OrdenCompra> listarOrdenesDeRepartidor(Long repartidorId) {
        return ordenCompraRepository.findByRepartidor_IdOrderByFechaAsignacionRepartidorDesc(repartidorId);
    }

    @Transactional
    public void marcarRecolectadaPorRepartidor(Long ordenId, Usuario repartidor) {
        OrdenCompra orden = obtenerPorId(ordenId);
        validarPermisoRepartidor(orden, repartidor);
        if (orden.getDeliveryStatus() == OrdenCompra.DeliveryStatus.READY_FOR_DISPATCH) {
            orden.setDeliveryStatus(OrdenCompra.DeliveryStatus.DISPATCHED);
            ordenCompraRepository.save(orden);
        } else {
            throw new IllegalStateException("La orden no está lista para despacho");
        }
    }

    @Transactional
    public void marcarEntregadaPorRepartidor(Long ordenId, Usuario repartidor) {
        OrdenCompra orden = obtenerPorId(ordenId);
        validarPermisoRepartidor(orden, repartidor);
        if (orden.getDeliveryStatus() == OrdenCompra.DeliveryStatus.DISPATCHED) {
            orden.setDeliveryStatus(OrdenCompra.DeliveryStatus.DELIVERED);
            ordenCompraRepository.save(orden);
        } else {
            throw new IllegalStateException("La orden no está en despacho");
        }
    }

    private void validarPermisoRepartidor(OrdenCompra orden, Usuario repartidor) {
        if (repartidor == null || repartidor.getRol() != Usuario.Rol.DELIVERY) {
            throw new AccessDeniedException("No autorizado: rol inválido");
        }
        if (orden.getRepartidor() == null || !Objects.equals(orden.getRepartidor().getId(), repartidor.getId())) {
            throw new AccessDeniedException("No autorizado: orden no asignada a este repartidor");
        }
    }
}
