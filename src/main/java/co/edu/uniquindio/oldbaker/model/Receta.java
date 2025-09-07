package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Receta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReceta;
    private String nombre;
    private String descripcion;
    private Integer cantidadInsumo;
    @ManyToOne
    @JoinColumn(name = "id_insumo")
    private Insumo insumo;
    @ManyToOne
    @JoinColumn(name = "id_producto")
    private Producto producto;
}
