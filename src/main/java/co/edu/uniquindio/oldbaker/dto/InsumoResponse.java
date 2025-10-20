package co.edu.uniquindio.oldbaker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsumoResponse {
    private Long idInsumo;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private Integer cantidadActual;
}
