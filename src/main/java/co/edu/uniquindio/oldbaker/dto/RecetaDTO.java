package co.edu.uniquindio.oldbaker.dto;

import co.edu.uniquindio.oldbaker.model.Receta;
import lombok.Data;

@Data
public class RecetaDTO {
    // Receta asociada
    private Long idReceta;
    private String insumoNombre;
    private Double cantidadInsumo;
    private Receta.UnidadMedida unidadMedida;
}
