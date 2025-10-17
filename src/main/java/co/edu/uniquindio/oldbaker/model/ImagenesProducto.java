package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "imagenes_productos")
public class ImagenesProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "imagen_producto_seq")
    @SequenceGenerator(name = "imagen_producto_seq", sequenceName = "IMAGEN_PRODUCTO_SEQ", allocationSize = 1)
    private Long id;
    private String url;
    private Long idProducto;
}
