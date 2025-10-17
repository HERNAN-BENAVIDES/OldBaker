package co.edu.uniquindio.oldbaker.repositories;

import co.edu.uniquindio.oldbaker.dto.ProductoHomeResponse;
import co.edu.uniquindio.oldbaker.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
        SELECT new co.edu.uniquindio.oldbaker.dto.ProductoHomeResponse(
            p.idProducto,
            p.nombre,
            p.descripcion,
            p.costoUnitario,
            p.vidaUtilDias,
            c.nombre,
            ip.url,
            p.pedidoMinimo
        )
        FROM Producto p
        LEFT JOIN Categoria c ON p.categoria.idCategoria = c.idCategoria
        LEFT JOIN ImagenesProducto ip ON p.idProducto = ip.idProducto
        WHERE p.categoria IS NOT NULL
        """)
    List<ProductoHomeResponse> findProductos();
}