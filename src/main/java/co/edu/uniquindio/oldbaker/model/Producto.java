package co.edu.uniquindio.oldbaker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private int vidaUtilDias;
    private int pedidoMinimo;
    @ManyToOne
    @JoinColumn(name = "categoria")
    @JsonIgnoreProperties({"productos"})
    private Categoria categoria;
}