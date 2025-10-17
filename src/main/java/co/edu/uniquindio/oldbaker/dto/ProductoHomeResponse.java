package co.edu.uniquindio.oldbaker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductoHomeResponse {
    private Long idProducto;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private int diasVidaUtil;
    private String categoriaNombre;
    private String url;
    private int pedidoMinimo;
}
