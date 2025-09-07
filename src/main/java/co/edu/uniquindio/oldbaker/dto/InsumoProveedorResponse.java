package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InsumoProveedorResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private LocalDate fechaVencimiento;
}
