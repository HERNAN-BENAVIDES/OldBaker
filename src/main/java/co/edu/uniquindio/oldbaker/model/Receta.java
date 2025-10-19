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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "receta_seq")
    @SequenceGenerator(name = "receta_seq", sequenceName = "RECETA_SEQ", allocationSize = 1)
    private Long idReceta;
    private String nombre;
    private String descripcion;
    private double cantidadInsumo;
    @Enumerated(EnumType.STRING)
    private UnidadMedida unidadMedida;
    @ManyToOne
    @JoinColumn(name = "id_insumo")
    private Insumo insumo;
    @ManyToOne
    @JoinColumn(name = "id_producto")
    private Producto producto;


    public enum UnidadMedida {
        GRAMOS,
        KILOGRAMOS,
        LITROS,
        MILILITROS,
        UNIDADES
    }


}