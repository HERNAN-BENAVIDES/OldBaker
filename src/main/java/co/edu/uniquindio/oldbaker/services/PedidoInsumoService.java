package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.*;
import co.edu.uniquindio.oldbaker.model.*;
import co.edu.uniquindio.oldbaker.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PedidoInsumoService {

    private final PedidoInsumoRepository pedidoInsumoRepository;
    private final InsumoRepository insumoRepository;
    private final PagoProveedorRepository pagoProveedorRepository;
    private final InsumoProveedorRepository insumoProveedorRepository;
    private final ReporteProveedorRepository reporteProveedorRepository;

    public PedidoInsumoService(PedidoInsumoRepository pedidoInsumoRepository,
                               InsumoRepository insumoRepository,
                               PagoProveedorRepository pagoProveedorRepository, InsumoProveedorRepository insumoProveedorRepository, ReporteProveedorRepository reporteProveedorRepository) {
        this.pedidoInsumoRepository = pedidoInsumoRepository;
        this.insumoRepository = insumoRepository;
        this.pagoProveedorRepository = pagoProveedorRepository;
        this.insumoProveedorRepository = insumoProveedorRepository;
        this.reporteProveedorRepository = reporteProveedorRepository;
    }

    // Crear un nuevo pedido (Admin)
    @Transactional
    public PedidoInsumoResponse crearPedido(PedidoInsumoRequest request) {
        PedidoInsumo pedido = new PedidoInsumo();
        pedido.setNombre(request.getNombre());
        pedido.setDescripcion(request.getDescripcion());
        pedido.setFechaPedido(request.getFechaPedido() != null ? request.getFechaPedido() : LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        BigDecimal total = BigDecimal.ZERO;

        // Procesar cada detalle que viene en el request
        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            List<DetalleProveedorPedido> detalles = new ArrayList<>();
            for (DetalleProveedorPedidoRequest detReq : request.getDetalles()) {
                InsumoProveedor insumoProveedor = insumoProveedorRepository.findById(detReq.getInsumoProveedorId())
                        .orElseThrow(() -> new RuntimeException("InsumoProveedor no encontrado: " + detReq.getInsumoProveedorId()));

                validarPrecioNegociado(detReq, insumoProveedor);

                BigDecimal cantidad = BigDecimal.valueOf(detReq.getCantidadInsumo());
                BigDecimal subtotal = detReq.getPrecioUnitario().multiply(cantidad);

                DetalleProveedorPedido detalle = new DetalleProveedorPedido();
                detalle.setCantidadInsumo(detReq.getCantidadInsumo());
                detalle.setPrecioUnitarioNegociado(detReq.getPrecioUnitario());
                detalle.setCostoSubtotal(subtotal);
                detalle.setEsDevuelto(false);
                detalle.setInsumo(insumoProveedor);
                detalle.setPedido(pedido);

                total = total.add(subtotal);
                detalles.add(detalle);
            }

            pedido.setDetalles(detalles);
        }

        pedido.setCostoTotal(total);

        PedidoInsumo guardado = pedidoInsumoRepository.save(pedido);

        return mapToResponse(guardado);
    }

    @Transactional
    public ReporteProveedorResponse devolverInsumo(Long idPedido, ReporteProveedorRequest request) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden devolver insumos de pedidos en estado PENDIENTE");
        }

        // Buscar el detalle dentro del pedido
        DetalleProveedorPedido detalle = pedido.getDetalles().stream()
                .filter(d -> d.getIdDetalle().equals(request.getDetalleId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado en este pedido"));

        if (Boolean.TRUE.equals(detalle.getEsDevuelto())) {
            throw new RuntimeException("Este insumo ya fue devuelto");
        }

        // Marcar detalle como devuelto
        detalle.setEsDevuelto(true);

        // Crear el reporte
        ReporteProveedor reporte = new ReporteProveedor();
        reporte.setRazon(request.getRazon());
        reporte.setEsDevolucion(true);
        reporte.setFechaDevolucion(LocalDate.now());
        reporte.setDetalle(detalle);

        reporteProveedorRepository.save(reporte);

        // Recalcular el costo total del pedido (sin el detalle devuelto)
        BigDecimal nuevoTotal = pedido.getDetalles().stream()
                .filter(d -> !Boolean.TRUE.equals(d.getEsDevuelto()))
                .map(DetalleProveedorPedido::getCostoSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setCostoTotal(nuevoTotal);
        pedidoInsumoRepository.save(pedido);

        // Respuesta
        ReporteProveedorResponse response = new ReporteProveedorResponse();
        response.setIdDevolucion(reporte.getIdDevolucion());
        response.setRazon(reporte.getRazon());
        response.setEsDevolucion(reporte.getEsDevolucion());
        response.setFechaDevolucion(reporte.getFechaDevolucion());
        response.setDetalleId(detalle.getIdDetalle());
        response.setInsumoNombre(detalle.getInsumo().getInsumo() != null
                ? detalle.getInsumo().getInsumo().getNombre() : null);
        response.setCantidadDevuelta(detalle.getCantidadInsumo());

        return response;
    }



    // Listar todos los pedidos
    public List<PedidoInsumoResponse> listarPedidos() {
        return pedidoInsumoRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Buscar pedido por id
    public PedidoInsumoResponse buscarPorId(Long id) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        return mapToResponse(pedido);
    }

    // Actualizar pedido (antes de ser aprobado)
    @Transactional
    public PedidoInsumoResponse actualizarPedido(Long id, PedidoInsumoRequest request) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden actualizar pedidos en estado PENDIENTE");
        }

        pedido.setNombre(request.getNombre());
        pedido.setDescripcion(request.getDescripcion());
        pedido.setFechaPedido(request.getFechaPedido());

        // Primero limpiamos los detalles anteriores (orphanRemoval = true los elimina de BD)
        if (pedido.getDetalles() == null) {
            pedido.setDetalles(new ArrayList<>());
        }
        pedido.getDetalles().clear();

        BigDecimal total = BigDecimal.ZERO;

        // Ahora agregamos los nuevos detalles desde el request
        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            List<DetalleProveedorPedido> nuevosDetalles = new ArrayList<>();
            for (DetalleProveedorPedidoRequest detReq : request.getDetalles()) {
                InsumoProveedor insumoProveedor = insumoProveedorRepository.findById(detReq.getInsumoProveedorId())
                        .orElseThrow(() -> new RuntimeException("InsumoProveedor no encontrado: " + detReq.getInsumoProveedorId()));

                validarPrecioNegociado(detReq, insumoProveedor);

                BigDecimal cantidad = BigDecimal.valueOf(detReq.getCantidadInsumo());
                BigDecimal subtotal = detReq.getPrecioUnitario().multiply(cantidad);

                DetalleProveedorPedido detalle = new DetalleProveedorPedido();
                detalle.setCantidadInsumo(detReq.getCantidadInsumo());
                detalle.setPrecioUnitarioNegociado(detReq.getPrecioUnitario());
                detalle.setCostoSubtotal(subtotal);
                detalle.setEsDevuelto(false);
                detalle.setInsumo(insumoProveedor);
                detalle.setPedido(pedido);

                total = total.add(subtotal);
                nuevosDetalles.add(detalle);
            }

            pedido.getDetalles().addAll(nuevosDetalles);
        }

        pedido.setCostoTotal(total);

        PedidoInsumo actualizado = pedidoInsumoRepository.save(pedido);
        return mapToResponse(actualizado);
    }


    // Eliminar pedido
    public void eliminarPedido(Long id) {
        if (!pedidoInsumoRepository.existsById(id)) {
            throw new RuntimeException("Pedido no encontrado");
        }
        pedidoInsumoRepository.deleteById(id);
    }

    // Aprobar pedido (Auxiliar)
    public PedidoInsumoResponse aprobarPedido(Long idPedido) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar pedidos en estado PENDIENTE");
        }

        pedido.setEstado(EstadoPedido.APROBADO);
        pedido.setFechaAprobacion(LocalDateTime.now());

        // Mapear insumos del proveedor al inventario de la empresa
        for (DetalleProveedorPedido detalle : pedido.getDetalles()) {
            InsumoProveedor insumoProveedor = detalle.getInsumo();

            Insumo insumo = insumoProveedor.getInsumo();
            if (insumo == null) {
                throw new RuntimeException("El insumo asociado al proveedor no está configurado correctamente");
            }

            BigDecimal cantidadActual = insumo.getCantidadActual() != null ? insumo.getCantidadActual() : BigDecimal.ZERO;
            BigDecimal nuevaCantidad = cantidadActual.add(BigDecimal.valueOf(detalle.getCantidadInsumo()));
            insumo.setCantidadActual(nuevaCantidad);
            insumoRepository.save(insumo);

            Integer disponible = insumoProveedor.getCantidadDisponible();
            if (disponible != null) {
                int restante = Math.max(0, disponible - detalle.getCantidadInsumo());
                insumoProveedor.setCantidadDisponible(restante);
                insumoProveedorRepository.save(insumoProveedor);
            }
        }

        pedido.setFechaRecepcion(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.RECIBIDO);

        PedidoInsumo actualizado = pedidoInsumoRepository.save(pedido);
        return mapToResponse(actualizado);
    }

    // Pagar pedido (Admin)
    public PedidoInsumoResponse pagarPedido(Long idPedido) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoPedido.RECIBIDO) {
            throw new RuntimeException("Solo se pueden pagar pedidos en estado RECIBIDO");
        }

        PagoProveedor pago = new PagoProveedor();
        pago.setDescripcion("Pago de pedido " + pedido.getNombre());
        pago.setMonto(pedido.getCostoTotal());
        pago.setFechaPago(LocalDate.now());
        pago.setPedido(pedido);

        pagoProveedorRepository.save(pago);

        pedido.setPago(pago);
        pedido.setEstado(EstadoPedido.PAGADO);
        pedido.setFechaPago(LocalDateTime.now());

        PedidoInsumo actualizado = pedidoInsumoRepository.save(pedido);
        return mapToResponse(actualizado);
    }

    // 🔹 Mapper interno (Entidad → DTO)
    private PedidoInsumoResponse mapToResponse(PedidoInsumo pedido) {
        PedidoInsumoResponse response = new PedidoInsumoResponse();
        response.setId(pedido.getIdPedido());
        response.setNombre(pedido.getNombre());
        response.setDescripcion(pedido.getDescripcion());
        response.setCostoTotal(pedido.getCostoTotal());
        response.setFechaPedido(pedido.getFechaPedido());
        response.setEstado(pedido.getEstado());
        response.setFechaAprobacion(pedido.getFechaAprobacion());
        response.setFechaRecepcion(pedido.getFechaRecepcion());
        response.setFechaPago(pedido.getFechaPago());
        response.setPago(pedido.getPago());

        if (pedido.getDetalles() != null) {
            response.setDetalles(
                    pedido.getDetalles().stream().map(detalle -> {
                        DetalleProveedorPedidoResponse detalleResponse = new DetalleProveedorPedidoResponse();
                        detalleResponse.setId(detalle.getIdDetalle());
                        detalleResponse.setCantidadInsumo(detalle.getCantidadInsumo());
                        detalleResponse.setPrecioUnitarioNegociado(detalle.getPrecioUnitarioNegociado());
                        detalleResponse.setCostoSubtotal(detalle.getCostoSubtotal());
                        detalleResponse.setEsDevuelto(detalle.getEsDevuelto());

                        InsumoProveedor insumoProveedor = detalle.getInsumo();
                        if (insumoProveedor != null) {
                            detalleResponse.setInsumo(mapInsumoProveedor(insumoProveedor));
                        }

                        return detalleResponse;
                    }).toList()
            );
        }

        return response;
    }

    private void validarPrecioNegociado(DetalleProveedorPedidoRequest detReq, InsumoProveedor insumoProveedor) {
        if (detReq.getPrecioUnitario().compareTo(insumoProveedor.getCostoUnitario()) < 0) {
            throw new RuntimeException("El precio negociado no puede ser inferior al costo base del proveedor");
        }
    }

    private InsumoProveedorResponse mapInsumoProveedor(InsumoProveedor insumoProveedor) {
        InsumoProveedorResponse insumoResp = new InsumoProveedorResponse();
        insumoResp.setId(insumoProveedor.getId());
        if (insumoProveedor.getInsumo() != null) {
            insumoResp.setInsumoId(insumoProveedor.getInsumo().getIdInsumo());
            insumoResp.setInsumoNombre(insumoProveedor.getInsumo().getNombre());
        }
        insumoResp.setCostoUnitario(insumoProveedor.getCostoUnitario());
        insumoResp.setCantidadDisponible(insumoProveedor.getCantidadDisponible());
        insumoResp.setFechaVigenciaDesde(insumoProveedor.getFechaVigenciaDesde());
        insumoResp.setFechaVigenciaHasta(insumoProveedor.getFechaVigenciaHasta());
        if (insumoProveedor.getProveedor() != null) {
            insumoResp.setProveedorId(insumoProveedor.getProveedor().getIdProveedor());
            insumoResp.setProveedorNombre(insumoProveedor.getProveedor().getNombre());
        }
        return insumoResp;
    }
}
