package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InsumoProveedorResponse {
    private Long id;
    private Long insumoId;
    private String insumoNombre;
    private BigDecimal costoUnitario;
    private Integer cantidadDisponible;
    private LocalDate fechaVigenciaDesde;
    private LocalDate fechaVigenciaHasta;
    private Long proveedorId;
    private String proveedorNombre;
}

