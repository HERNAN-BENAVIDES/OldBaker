package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos_insumos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PedidoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idPedido;

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotBlank
    @Size(max = 255)
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 12, fraction = 2)
    private BigDecimal costoTotal;

    @NotNull
    private LocalDate fechaPedido;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EstadoPedido estado;

    private LocalDateTime fechaAprobacion;

    private LocalDateTime fechaRecepcion;

    private LocalDateTime fechaPago;

    @OneToOne
    @JoinColumn(name = "id_pago")
    private PagoProveedor pago;

    // Relación con detalle (un pedido tiene muchos detalles)
    @Builder.Default
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleProveedorPedido> detalles = new ArrayList<>();
}

