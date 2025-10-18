package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.payment.CheckoutItemDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutRequestDTO;
import co.edu.uniquindio.oldbaker.model.*;
import co.edu.uniquindio.oldbaker.repositories.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private static final Logger logger = LoggerFactory.getLogger(OrdenCompraService.class);

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProductRepository productRepository;
    private final UsuarioRepository usuarioRepository;
    private final InsumoRepository insumoRepository;
    private final RecetaRepository recetaRepository;

    /**
     * Crea una orden en estado PENDING antes de redirigir a MercadoPago.
     * Calcula el total desde la BD (precios confiables) y genera external_reference único.
     */
    @Transactional
    public OrdenCompra crearOrden(CheckoutRequestDTO request, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioId));

        OrdenCompra orden = OrdenCompra.builder()
                .externalReference(UUID.randomUUID().toString())
                .status(OrdenCompra.EstadoOrden.PENDING)
                .usuario(usuario)
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
        if (orden.getStatus() == OrdenCompra.EstadoOrden.PAID) {
            if (paymentId.equals(orden.getPaymentId())) {
                logger.info("Orden {} ya está marcada como PAID con el mismo paymentId={}, ignorando webhook duplicado",
                        orden.getId(), paymentId);
            } else {
                logger.warn("Orden {} ya está PAID pero con diferente paymentId. Anterior: {}, Nuevo: {}",
                        orden.getId(), orden.getPaymentId(), paymentId);
            }
            return;
        }

        // Verificar transiciones de estado válidas
        if (orden.getStatus() == OrdenCompra.EstadoOrden.CANCELLED) {
            logger.error("Intento de marcar como PAID una orden CANCELADA: ordenId={} paymentId={}",
                    orden.getId(), paymentId);
            throw new IllegalStateException("No se puede marcar como PAID una orden cancelada");
        }

        logger.info("Iniciando proceso de pago para orden {}: PENDING/IN_PROCESS -> PAID", orden.getId());

        orden.setStatus(OrdenCompra.EstadoOrden.PAID);
        orden.setPaymentId(paymentId);

        // Descontar stock de insumos (en la misma transacción)
        descontarStock(orden);

        ordenCompraRepository.save(orden);
        logger.info("Orden {} marcada como PAID exitosamente, stock descontado, paymentId={}",
                orden.getId(), paymentId);
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
        if (orden.getStatus() == OrdenCompra.EstadoOrden.FAILED) {
            if (paymentId != null && paymentId.equals(orden.getPaymentId())) {
                logger.info("Orden {} ya está marcada como FAILED con el mismo paymentId={}, ignorando",
                        orden.getId(), paymentId);
                return;
            }
        }

        // Si ya está PAID, no permitir cambio a FAILED
        if (orden.getStatus() == OrdenCompra.EstadoOrden.PAID) {
            logger.error("Intento de marcar como FAILED una orden ya PAID: ordenId={} paymentId={}",
                    orden.getId(), paymentId);
            throw new IllegalStateException("No se puede marcar como FAILED una orden ya pagada");
        }

        orden.setStatus(OrdenCompra.EstadoOrden.FAILED);
        orden.setPaymentId(paymentId);
        ordenCompraRepository.save(orden);

        logger.info("Orden {} marcada como FAILED, paymentId={}", orden.getId(), paymentId);
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
        if (orden.getStatus() == OrdenCompra.EstadoOrden.PAID) {
            logger.info("Orden {} ya está PAID, ignorando actualización a IN_PROCESS", orden.getId());
            return;
        }

        // Idempotencia: si ya está IN_PROCESS con el mismo paymentId, ignorar
        if (orden.getStatus() == OrdenCompra.EstadoOrden.IN_PROCESS) {
            if (paymentId != null && paymentId.equals(orden.getPaymentId())) {
                logger.info("Orden {} ya está en IN_PROCESS con el mismo paymentId={}, ignorando",
                        orden.getId(), paymentId);
                return;
            }
        }

        orden.setStatus(OrdenCompra.EstadoOrden.IN_PROCESS);
        orden.setPaymentId(paymentId);
        ordenCompraRepository.save(orden);

        logger.info("Orden {} marcada como IN_PROCESS, paymentId={}", orden.getId(), paymentId);
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
}
