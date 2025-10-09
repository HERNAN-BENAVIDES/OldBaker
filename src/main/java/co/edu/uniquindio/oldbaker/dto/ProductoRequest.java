package co.edu.uniquindio.oldbaker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductoRequest {
    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal costoUnitario;

    @NotNull
    @Future
    private LocalDate fechaVencimiento;

    @NotNull
    private Long categoriaId;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal stockInicial;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal stockMinimo;

    // Receta
    @NotNull
    private Long insumoId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal cantidadInsumo; // cuántos insumos necesita cada producto

    @NotNull
    @Positive
    private Integer cantidadProductos; // cuántos productos se desean crear
}
