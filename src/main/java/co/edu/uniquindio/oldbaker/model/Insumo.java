package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insumos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInsumo;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private Integer cantidadActual;
    @OneToOne
    @JoinColumn(name = "id_insumo_proveedor", nullable = false, unique = true)
    private InsumoProveedor insumoProveedor;
}

