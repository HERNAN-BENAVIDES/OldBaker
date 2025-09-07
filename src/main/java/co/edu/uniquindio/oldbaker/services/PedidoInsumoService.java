package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.DetallePedidoResponse;
import co.edu.uniquindio.oldbaker.dto.InsumoProveedorResponse;
import co.edu.uniquindio.oldbaker.dto.PedidoInsumoResponse;
import co.edu.uniquindio.oldbaker.model.*;
import co.edu.uniquindio.oldbaker.repositories.InsumoRepository;
import co.edu.uniquindio.oldbaker.repositories.PagoProveedorRepository;
import co.edu.uniquindio.oldbaker.repositories.PedidoInsumoRepository;
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

    public PedidoInsumoService(PedidoInsumoRepository pedidoInsumoRepository, InsumoRepository insumoRepository, PagoProveedorRepository pagoProveedorRepository) {
        this.pedidoInsumoRepository = pedidoInsumoRepository;
        this.insumoRepository = insumoRepository;
        this.pagoProveedorRepository = pagoProveedorRepository;
    }

    // Crear un nuevo pedido (lo hace el administrador)
    public PedidoInsumo crearPedido(PedidoInsumo pedido) {
        pedido.setEstado(EstadoPedido.PENDIENTE);
        return pedidoInsumoRepository.save(pedido);
    }

    // Obtener todos los pedidos
    public List<PedidoInsumo> listarPedidos() {
        return pedidoInsumoRepository.findAll();
    }

    // Buscar pedido por id
    public Optional<PedidoInsumo> buscarPorId(Long id) {
        return pedidoInsumoRepository.findById(id);
    }

    public PedidoInsumo actualizarPedido(Long id, PedidoInsumo pedidoActualizado) {
        return pedidoInsumoRepository.findById(id).map(pedido -> {
            pedido.setNombre(pedidoActualizado.getNombre());
            pedido.setDescripcion(pedidoActualizado.getDescripcion());
            pedido.setCostoTotal(pedidoActualizado.getCostoTotal());
            pedido.setFechaPedido(pedidoActualizado.getFechaPedido());
            pedido.setEstado(pedidoActualizado.getEstado());
            pedido.setPago(pedidoActualizado.getPago());
            return pedidoInsumoRepository.save(pedido);
        }).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    // Eliminar pedido
    public void eliminarPedido(Long id) {
        if (!pedidoInsumoRepository.existsById(id)) {
            throw new RuntimeException("Pedido no encontrado");
        }
        pedidoInsumoRepository.deleteById(id);
    }


    public PedidoInsumoResponse aprobarPedido(Long idPedido) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar pedidos en estado PENDIENTE");
        }

        pedido.setEstado(EstadoPedido.APROBADO);

        for (DetalleProveedorPedido detalle : pedido.getDetalles()) {
            InsumoProveedor insumoProveedor = detalle.getInsumo();

            // Buscar si ya existe en inventario
            Optional<Insumo> existente = insumoRepository.findByInsumoProveedor(insumoProveedor);

            if (existente.isPresent()) {
                // Si existe → acumular la cantidad
                Insumo insumo = existente.get();
                insumo.setCantidadActual(insumo.getCantidadActual() + detalle.getCantidadInsumo());
                insumoRepository.save(insumo);
            } else {
                // Si no existe → crear nuevo insumo ligado al insumoProveedor
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

    @Transactional
    public PedidoInsumoResponse pagarPedido(Long idPedido) {
        PedidoInsumo pedido = pedidoInsumoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoPedido.APROBADO) {
            throw new RuntimeException("Solo se pueden pagar pedidos en estado APROBADO");
        }

        // Crear el pago en base al pedido
        PagoProveedor pago = new PagoProveedor();
        pago.setMonto(pedido.getCostoTotal());
        pago.setFechaPago(LocalDate.now());
        pago.setPedido(pedido);

        pagoProveedorRepository.save(pago);

        // Asociar y actualizar estado del pedido
        pedido.setPago(pago);
        pedido.setEstado(EstadoPedido.PAGADO);

        PedidoInsumo actualizado = pedidoInsumoRepository.save(pedido);

        return mapToResponse(actualizado);
    }



    private PedidoInsumoResponse mapToResponse(PedidoInsumo pedido) {
        PedidoInsumoResponse response = new PedidoInsumoResponse();

        // Asignar los valores principales del pedido
        response.setId(pedido.getIdPedido());
        response.setNombre(pedido.getNombre());
        response.setDescripcion(pedido.getDescripcion());
        response.setCostoTotal(pedido.getCostoTotal());
        response.setFechaPedido(pedido.getFechaPedido());
        response.setEstado(pedido.getEstado());
        response.setPago(pedido.getPago());

        // Mapear los detalles del pedido
        if (pedido.getDetalles() != null) {
            response.setDetalles(
                    pedido.getDetalles().stream().map(detalle -> {
                        DetallePedidoResponse detalleResponse = new DetallePedidoResponse();
                        detalleResponse.setId(detalle.getIdDetalle());
                        detalleResponse.setCantidadInsumo(detalle.getCantidadInsumo());
                        detalleResponse.setCostoSubtotal(detalle.getCostoSubtotal());

                        // Extraer información del insumo proveedor
                        InsumoProveedorResponse insumoProveedorResponse = new InsumoProveedorResponse();
                        InsumoProveedor insumoProveedor = detalle.getInsumo();
                        if (insumoProveedor != null) {
                            insumoProveedorResponse.setId(insumoProveedor.getIdInsumo());
                            insumoProveedorResponse.setNombre(insumoProveedor.getNombre());
                            insumoProveedorResponse.setDescripcion(insumoProveedor.getDescripcion());
                            insumoProveedorResponse.setCostoUnitario(insumoProveedor.getCostoUnitario());
                            insumoProveedorResponse.setFechaVencimiento(insumoProveedor.getFechaVencimiento());
                        }

                        detalleResponse.setInsumo(insumoProveedorResponse);
                        return detalleResponse;
                    }).toList()
            );
        }

        return response;
    }


}
