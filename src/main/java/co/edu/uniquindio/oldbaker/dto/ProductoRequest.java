package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

@Data
public class ProductoRequest {
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private String fechaVencimiento;
    private Long categoriaId;

    // Receta
    private Long insumoId;
    private Integer cantidadInsumo; // cuántos insumos necesita cada producto
    private Integer cantidadProductos; // cuántos productos se desean crear
}
