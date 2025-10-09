package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.ProductoRequest;
import co.edu.uniquindio.oldbaker.dto.ProductoResponse;
import co.edu.uniquindio.oldbaker.model.Categoria;
import co.edu.uniquindio.oldbaker.model.Insumo;
import co.edu.uniquindio.oldbaker.model.Producto;
import co.edu.uniquindio.oldbaker.model.Receta;
import co.edu.uniquindio.oldbaker.repositories.CategoriaRepository;
import co.edu.uniquindio.oldbaker.repositories.InsumoRepository;
import co.edu.uniquindio.oldbaker.repositories.ProductoRepository;
import co.edu.uniquindio.oldbaker.repositories.RecetaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final RecetaRepository recetaRepository;
    private final InsumoRepository insumoRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional
    public ProductoResponse crearProductoConReceta(ProductoRequest request) {
        // Validar insumo
        Insumo insumo = insumoRepository.findById(request.getInsumoId())
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));

        BigDecimal insumosNecesarios = request.getCantidadInsumo()
                .multiply(BigDecimal.valueOf(request.getCantidadProductos()));
        if (insumo.getCantidadActual().compareTo(insumosNecesarios) < 0) {
            throw new RuntimeException("No hay insumos suficientes. Disponibles: "
                    + insumo.getCantidadActual() + ", requeridos: " + insumosNecesarios);
        }

        // Descontar insumos
        insumo.setCantidadActual(insumo.getCantidadActual().subtract(insumosNecesarios));
        insumoRepository.save(insumo);

        // Buscar categoría
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Crear producto
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setCostoUnitario(request.getCostoUnitario());
        producto.setFechaVencimiento(request.getFechaVencimiento());
        producto.setCategoria(categoria);
        producto.setStockActual(request.getStockInicial());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setFechaUltimaProduccion(LocalDateTime.now());
        Producto productoGuardado = productoRepository.save(producto);

        // Crear receta asociada
        Receta receta = new Receta();
        receta.setNombre("Receta de " + producto.getNombre());
        receta.setDescripcion("Receta automática para " + producto.getNombre());
        receta.setCantidadInsumo(request.getCantidadInsumo());
        receta.setUnidadesProducidas(request.getCantidadProductos());
        receta.setInsumo(insumo);
        receta.setProducto(productoGuardado);
        recetaRepository.save(receta);

        // Respuesta
        ProductoResponse response = new ProductoResponse();
        response.setIdProducto(productoGuardado.getIdProducto());
        response.setNombre(productoGuardado.getNombre());
        response.setDescripcion(productoGuardado.getDescripcion());
        response.setCostoUnitario(productoGuardado.getCostoUnitario());
        response.setFechaVencimiento(productoGuardado.getFechaVencimiento());
        response.setStockActual(productoGuardado.getStockActual());
        response.setStockMinimo(productoGuardado.getStockMinimo());
        response.setUltimaProduccion(productoGuardado.getFechaUltimaProduccion() != null
                ? productoGuardado.getFechaUltimaProduccion().toLocalDate() : null);
        response.setCategoriaNombre(productoGuardado.getCategoria().getNombre());
        response.setIdReceta(receta.getIdReceta());
        response.setInsumoNombre(insumo.getNombre());
        response.setCantidadInsumo(receta.getCantidadInsumo());
        response.setUnidadesProducidas(receta.getUnidadesProducidas());

        return response;
    }

    public List<ProductoResponse> listarProductos() {
        return productoRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public ProductoResponse obtenerProductoPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return mapToResponse(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        productoRepository.delete(producto);
    }

    // Mapper reutilizable
    private ProductoResponse mapToResponse(Producto producto) {
        ProductoResponse response = new ProductoResponse();
        response.setIdProducto(producto.getIdProducto());
        response.setNombre(producto.getNombre());
        response.setDescripcion(producto.getDescripcion());
        response.setCostoUnitario(producto.getCostoUnitario());
        response.setFechaVencimiento(producto.getFechaVencimiento());
        response.setStockActual(producto.getStockActual());
        response.setStockMinimo(producto.getStockMinimo());
        response.setUltimaProduccion(producto.getFechaUltimaProduccion() != null
                ? producto.getFechaUltimaProduccion().toLocalDate() : null);
        response.setCategoriaNombre(producto.getCategoria().getNombre());

        // Buscar receta por producto
        Receta receta = recetaRepository.findByProducto(producto);
        if (receta != null) {
            response.setIdReceta(receta.getIdReceta());
            response.setInsumoNombre(receta.getInsumo().getNombre());
            response.setCantidadInsumo(receta.getCantidadInsumo());
            response.setUnidadesProducidas(receta.getUnidadesProducidas());
        }

        return response;
    }

}

