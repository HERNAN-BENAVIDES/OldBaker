package co.edu.uniquindio.oldbaker.dto;

import co.edu.uniquindio.oldbaker.model.EstadoPedido;
import co.edu.uniquindio.oldbaker.model.PagoProveedor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PedidoInsumoResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal costoTotal;
    private LocalDate fechaPedido;
    private EstadoPedido estado;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaRecepcion;
    private LocalDateTime fechaPago;
    private PagoProveedor pago;
    private List<DetalleProveedorPedidoResponse> detalles;

}
