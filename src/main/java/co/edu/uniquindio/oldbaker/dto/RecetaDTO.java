package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

@Data
public class RecetaDTO {
    // Receta asociada
    private Long idReceta;
    private String insumoNombre;
    private Double cantidadInsumo;
}
