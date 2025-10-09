package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductoResponse {
    private Long idProducto;
    private String nombre;
    private String descripcion;
    private BigDecimal costoUnitario;
    private LocalDate fechaVencimiento;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private LocalDate ultimaProduccion;
    private String categoriaNombre;

    // Receta asociada
    private Long idReceta;
    private String insumoNombre;
    private BigDecimal cantidadInsumo;
    private Integer unidadesProducidas;
}

