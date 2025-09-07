package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleProveedorPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalle;
    private Integer cantidadInsumo;
    private Double costoSubtotal;
    private Boolean esDevuelto;
    @ManyToOne
    @JoinColumn(name = "id_insumo")
    private InsumoProveedor insumoProveedor;
    @ManyToOne
    @JoinColumn(name = "id_pedido")
    private PedidoInsumo pedido;
}
