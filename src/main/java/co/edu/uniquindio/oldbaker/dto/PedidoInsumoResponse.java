package co.edu.uniquindio.oldbaker.dto;

import co.edu.uniquindio.oldbaker.model.PagoProveedor;
import co.edu.uniquindio.oldbaker.model.PedidoInsumo;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PedidoInsumoResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Double costoTotal;
    private LocalDate fechaPedido;
    private PedidoInsumo.EstadoPedido estado;
    private PagoProveedor pago;
    private List<DetalleProveedorPedidoResponse> detalles;

}
