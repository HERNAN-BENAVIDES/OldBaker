package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetalleProveedorPedidoResponse {
    private Long id;
    private Integer cantidadInsumo;
    private BigDecimal precioUnitarioNegociado;
    private BigDecimal costoSubtotal;
    private Boolean esDevuelto;
    private InsumoProveedorResponse insumo;
}
