package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "reportes_a_proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteProveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDevolucion;
    private String razon;
    private Boolean esDevolucion;
    private LocalDate fechaDevolucion;
    @ManyToOne
    @JoinColumn(name = "id_detalle")
    private DetalleProveedorPedido detalle;
}
