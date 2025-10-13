package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_proveedor_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleProveedorPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "detalle_proveedor_pedido_seq")
    @SequenceGenerator(name = "detalle_proveedor_pedido_seq", sequenceName = "DETALLE_PROVEEDOR_PEDIDO_SEQ", allocationSize = 1)
    private Long idDetalle;

    private Integer cantidadInsumo;
    private Double costoSubtotal;
    private Boolean esDevuelto;

    // Relación con PedidoInsumo
    @ManyToOne
    @JoinColumn(name = "id_pedido")
    private PedidoInsumo pedido;

    // Relación con InsumoProveedor
    @ManyToOne
    @JoinColumn(name = "id_insumo")
    private InsumoProveedor insumo;
}