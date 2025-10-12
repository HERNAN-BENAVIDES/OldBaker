package co.edu.uniquindio.oldbaker.model;

import co.edu.uniquindio.oldbaker.model.InsumoProveedor;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insumos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insumo_seq")
    @SequenceGenerator(name = "insumo_seq", sequenceName = "INSUMO_SEQ", allocationSize = 1)
    private Long idInsumo;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private Integer cantidadActual;
    @OneToOne
    @JoinColumn(name = "id_insumo_proveedor", nullable = true, unique = true)
    private InsumoProveedor insumoProveedor;
}