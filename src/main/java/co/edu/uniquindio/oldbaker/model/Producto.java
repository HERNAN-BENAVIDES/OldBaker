package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_seq")
    @SequenceGenerator(name = "producto_seq", sequenceName = "PRODUCTO_SEQ", allocationSize = 1)
    private Long idProducto;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private String fechaVencimiento;
    @ManyToOne
    @JoinColumn(name = "categoria")
    private Categoria categoria;

}