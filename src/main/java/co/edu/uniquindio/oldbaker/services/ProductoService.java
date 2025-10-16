package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.ProductoHomeResponse;
import co.edu.uniquindio.oldbaker.dto.ProductoRequest;
import co.edu.uniquindio.oldbaker.dto.ProductoResponse;
import co.edu.uniquindio.oldbaker.dto.RecetaDTO;
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

        int insumosNecesarios = request.getCantidadInsumo() * request.getCantidadProductos();
        if (insumo.getCantidadActual() < insumosNecesarios) {
            throw new RuntimeException("No hay insumos suficientes. Disponibles: "
                    + insumo.getCantidadActual() + ", requeridos: " + insumosNecesarios);
        }

        // Descontar insumos
        insumo.setCantidadActual(insumo.getCantidadActual() - insumosNecesarios);
        insumoRepository.save(insumo);

        // Buscar categoría
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Crear producto
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setCostoUnitario(request.getCostoUnitario());
        producto.setVidaUtilDias(request.getDiasVidaUtil());
        producto.setCategoria(categoria);
        Producto productoGuardado = productoRepository.save(producto);

        // Crear receta asociada
        Receta receta = new Receta();
        receta.setNombre("Receta de " + producto.getNombre());
        receta.setDescripcion("Receta automática para " + producto.getNombre());
        receta.setCantidadInsumo(request.getCantidadInsumo());
        receta.setInsumo(insumo);
        receta.setProducto(productoGuardado);
        recetaRepository.save(receta);

        // Respuesta
        ProductoResponse response = new ProductoResponse();
        response.setIdProducto(productoGuardado.getIdProducto());
        response.setNombre(productoGuardado.getNombre());
        response.setDescripcion(productoGuardado.getDescripcion());
        response.setCostoUnitario(productoGuardado.getCostoUnitario());
        response.setVidaUtilDias(productoGuardado.getVidaUtilDias());
        response.setCategoriaNombre(productoGuardado.getCategoria().getNombre());
        response.setReceta(null);
        //response.setIdReceta(receta.getIdReceta());
        //response.setInsumoNombre(insumo.getNombre());
        //response.setCantidadInsumo(receta.getCantidadInsumo());

        return response;
    }

    public List<ProductoResponse> listarProductos() {
        return productoRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<ProductoHomeResponse> listarProductosHome() {
        return productoRepository.findProductos();
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
        response.setVidaUtilDias(producto.getVidaUtilDias());
        response.setCategoriaNombre(producto.getCategoria().getNombre());

        // Buscar receta por producto
        List<Receta> recetas = recetaRepository.findByProducto(producto);
        if (recetas != null) {

            response.setReceta(recetas.stream().map(r -> {;
                RecetaDTO dto = new RecetaDTO();
                dto.setIdReceta(r.getIdReceta());
                dto.setCantidadInsumo(r.getCantidadInsumo());
                dto.setInsumoNombre(r.getInsumo().getNombre());
                return dto;
            }).toList());
        }

        return response;
    }

}

