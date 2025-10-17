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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reporte_proverdor_seq")
    @SequenceGenerator(name = "reporte_proverdor_seq", sequenceName = "REPORTE_PROVEDOR_SEQ", allocationSize = 1)    private Long idDevolucion;
    private String razon;
    private Boolean esDevolucion;
    private LocalDate fechaDevolucion;
    @ManyToOne
    @JoinColumn(name = "id_detalle")
    private DetalleProveedorPedido detalle;
}
