package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

@Data
public class DetalleProveedorPedidoResponse {
    private Long id;
    private Integer cantidadInsumo;
    private Double costoSubtotal;
    private Boolean esDevuelto;
    private InsumoProveedorResponse insumo;
}
