package co.edu.uniquindio.oldbaker.dto;

import co.edu.uniquindio.oldbaker.model.EstadoPedido;
import co.edu.uniquindio.oldbaker.model.PagoProveedor;
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
    private EstadoPedido estado;
    private PagoProveedor pago;
    private List<DetalleProveedorPedidoResponse> detalles;

}
