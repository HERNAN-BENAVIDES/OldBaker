package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pagos_proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoProveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;
    private String descripcion;
    private Double monto;
}
