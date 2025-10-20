package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.*;
import co.edu.uniquindio.oldbaker.model.*;
import co.edu.uniquindio.oldbaker.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    public Long obtenerProveedorPorPedido(Long idPedido) {
        return pedidoInsumoRepository.findProveedorByPedidoId(idPedido);
    }

    // Crear un nuevo pedido (Admin)
    @Transactional
    public PedidoInsumoResponse crearPedido(PedidoInsumoRequest request) {
        PedidoInsumo pedido = new PedidoInsumo();
        pedido.setNombre(request.getNombre());
        pedido.setDescripcion(request.getDescripcion());
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(PedidoInsumo.EstadoPedido.PENDIENTE);

        final double[] total = {0.0};

        // Procesar cada detalle que viene en el request
        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            List<DetalleProveedorPedido> detalles = request.getDetalles().stream().map(detReq -> {
                // Buscar el insumoProveedor por id
                InsumoProveedor insumoProveedor = insumoProveedorRepository.findById(detReq.getInsumoProveedorId())
                        .orElseThrow(() -> new RuntimeException("InsumoProveedor no encontrado: " + detReq.getInsumoProveedorId()));

                // Calcular subtotal
                double subtotal = detReq.getCantidadInsumo() * insumoProveedor.getCostoUnitario();

                // Crear el detalle
                DetalleProveedorPedido detalle = new DetalleProveedorPedido();
                detalle.setCantidadInsumo(detReq.getCantidadInsumo());
                detalle.setCostoSubtotal(subtotal);
                detalle.setEsDevuelto(false);
                detalle.setInsumo(insumoProveedor);
                detalle.setPedido(pedido);

                // Acumular el total
                total[0] += subtotal;

                return detalle;
            }).toList();

            pedido.setDetalles(detalles);
        }

        pedido.setCostoTotal(total[0]);

        PedidoInsumo guardado = pedidoInsumoRepository.save(pedido);

        return mapToResponse(guardado);
    }

    @Transactional
    public ReporteProveedorResponse devolverInsumo(Long idPedido, ReporteProveedorRequest request) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != PedidoInsumo.EstadoPedido.PENDIENTE) {
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
        double nuevoTotal = pedido.getDetalles().stream()
                .filter(d -> !Boolean.TRUE.equals(d.getEsDevuelto()))
                .mapToDouble(DetalleProveedorPedido::getCostoSubtotal)
                .sum();

        pedido.setCostoTotal(nuevoTotal);
        pedidoInsumoRepository.save(pedido);

        // Respuesta
        ReporteProveedorResponse response = new ReporteProveedorResponse();
        response.setIdDevolucion(reporte.getIdDevolucion());
        response.setRazon(reporte.getRazon());
        response.setEsDevolucion(reporte.getEsDevolucion());
        response.setFechaDevolucion(reporte.getFechaDevolucion());
        response.setDetalleId(detalle.getIdDetalle());
        response.setInsumoNombre(detalle.getInsumo().getNombre());
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

        if (pedido.getEstado() != PedidoInsumo.EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden actualizar pedidos en estado PENDIENTE");
        }

        pedido.setNombre(request.getNombre());
        pedido.setDescripcion(request.getDescripcion());
        pedido.setFechaPedido(request.getFechaPedido());

        // Primero limpiamos los detalles anteriores (orphanRemoval = true los elimina de BD)
        pedido.getDetalles().clear();

        final double[] total = {0.0};

        // Ahora agregamos los nuevos detalles desde el request
        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            List<DetalleProveedorPedido> nuevosDetalles = request.getDetalles().stream().map(detReq -> {
                InsumoProveedor insumoProveedor = insumoProveedorRepository.findById(detReq.getInsumoProveedorId())
                        .orElseThrow(() -> new RuntimeException("InsumoProveedor no encontrado: " + detReq.getInsumoProveedorId()));

                double subtotal = detReq.getCantidadInsumo() * insumoProveedor.getCostoUnitario();

                DetalleProveedorPedido detalle = new DetalleProveedorPedido();
                detalle.setCantidadInsumo(detReq.getCantidadInsumo());
                detalle.setCostoSubtotal(subtotal);
                detalle.setEsDevuelto(false);
                detalle.setInsumo(insumoProveedor);
                detalle.setPedido(pedido);

                total[0] += subtotal;
                return detalle;
            }).toList();

            pedido.getDetalles().addAll(nuevosDetalles);
        }

        pedido.setCostoTotal(total[0]);

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

        if (pedido.getEstado() != PedidoInsumo.EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar pedidos en estado PENDIENTE");
        }

        pedido.setEstado(PedidoInsumo.EstadoPedido.APROBADO);

        // Mapear insumos del proveedor al inventario de la empresa
        for (DetalleProveedorPedido detalle : pedido.getDetalles()) {
            InsumoProveedor insumoProveedor = detalle.getInsumo();

            Optional<Insumo> existente = insumoRepository.findByInsumoProveedor(insumoProveedor);

            if (existente.isPresent()) {
                Insumo insumo = existente.get();
                insumo.setCantidadActual(insumo.getCantidadActual() + detalle.getCantidadInsumo());
                insumoRepository.save(insumo);
            } else {
                Insumo nuevo = new Insumo();
                nuevo.setNombre(insumoProveedor.getNombre());
                nuevo.setDescripcion(insumoProveedor.getDescripcion());
                nuevo.setCostoUnitario(insumoProveedor.getCostoUnitario());
                nuevo.setCantidadActual(detalle.getCantidadInsumo());
                nuevo.setInsumoProveedor(insumoProveedor);
                insumoRepository.save(nuevo);
            }
        }

        PedidoInsumo actualizado = pedidoInsumoRepository.save(pedido);
        return mapToResponse(actualizado);
    }

    // Pagar pedido (Admin)
    public PedidoInsumoResponse pagarPedido(Long idPedido) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != PedidoInsumo.EstadoPedido.APROBADO) {
            throw new RuntimeException("Solo se pueden pagar pedidos en estado APROBADO");
        }

        PagoProveedor pago = new PagoProveedor();
        pago.setMonto(pedido.getCostoTotal());
        pago.setFechaPago(LocalDate.now());
        pago.setPedido(pedido);

        pagoProveedorRepository.save(pago);

        pedido.setPago(pago);
        pedido.setEstado(PedidoInsumo.EstadoPedido.PAGADO);

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
        response.setPago(pedido.getPago());

        if (pedido.getDetalles() != null) {
            response.setDetalles(
                    pedido.getDetalles().stream().map(detalle -> {
                        DetalleProveedorPedidoResponse detalleResponse = new DetalleProveedorPedidoResponse();
                        detalleResponse.setId(detalle.getIdDetalle());
                        detalleResponse.setCantidadInsumo(detalle.getCantidadInsumo());
                        detalleResponse.setCostoSubtotal(detalle.getCostoSubtotal());

                        InsumoProveedor insumoProveedor = detalle.getInsumo();
                        if (insumoProveedor != null) {
                            InsumoProveedorResponse insumoResp = new InsumoProveedorResponse();
                            insumoResp.setId(insumoProveedor.getIdInsumo());
                            insumoResp.setNombre(insumoProveedor.getNombre());
                            insumoResp.setDescripcion(insumoProveedor.getDescripcion());
                            insumoResp.setCostoUnitario(insumoProveedor.getCostoUnitario());
                            insumoResp.setFechaVencimiento(insumoProveedor.getFechaVencimiento());
                            detalleResponse.setInsumo(insumoResp);
                        }

                        return detalleResponse;
                    }).toList()
            );
        }

        return response;
    }
}
