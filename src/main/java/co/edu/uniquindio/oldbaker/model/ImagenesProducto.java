package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "imagenes_productos")
public class ImagenesProducto {
    @Id
    private Long id;
    private String url;
    private Long idProducto;
}
