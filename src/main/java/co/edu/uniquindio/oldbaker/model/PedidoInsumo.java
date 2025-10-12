package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "pedidos_insumos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedido_insumo_seq")
    @SequenceGenerator(name = "pedido_insumo_seq", sequenceName = "PEDIDO_INSUMO_SEQ", allocationSize = 1)
    private Long idPedido;

    private String nombre;
    private String descripcion;
    private Double costoTotal;
    private LocalDate fechaPedido;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    @OneToOne
    @JoinColumn(name = "id_pago")
    private PagoProveedor pago;

    // Relación con detalle (un pedido tiene muchos detalles)
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleProveedorPedido> detalles = new ArrayList<>();


    public enum EstadoPedido {
        PENDIENTE,
        APROVADO,
        RECHAZADO,
        PAGADO
    }
}