package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "pedidos_insumos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoInsumo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedido;
    private String nombre;
    private String descripcion;
    private Double costoTotal;
    private Date fechaPedido;
    private Boolean esPagable;
    @ManyToOne
    @JoinColumn(name = "id_pago")
    private PagoProveedor pago;
}
