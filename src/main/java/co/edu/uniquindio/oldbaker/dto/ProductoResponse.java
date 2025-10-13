package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

@Data
public class ProductoResponse {
    private Long idProducto;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private String fechaVencimiento;
    private String categoriaNombre;

    // Receta asociada
    private Long idReceta;
    private String insumoNombre;
    private Double cantidadInsumo;
}

