package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReporteProveedorResponse {

    private Long idDevolucion;
    private String razon;
    private Boolean esDevolucion;
    private LocalDate fechaDevolucion;
    private Long idProveedor;

    private Long detalleId;
    private String insumoNombre;
    private Integer cantidadDevuelta;
}

