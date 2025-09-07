package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

@Data
public class DetallePedidoResponse {
    private Long id;
    private Integer cantidadInsumo;
    private Double costoSubtotal;
    private InsumoProveedorResponse insumo;
}
