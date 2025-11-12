package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.cart.CartDTO;
import co.edu.uniquindio.oldbaker.dto.cart.CartItemDTO;
import co.edu.uniquindio.oldbaker.model.*;
import co.edu.uniquindio.oldbaker.repositories.CartRepository;
import co.edu.uniquindio.oldbaker.repositories.ProductoRepository;
import co.edu.uniquindio.oldbaker.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public CartDTO obtenerCart(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) {
            return null;
        }
        Cart cart = cartRepository.findByUsuario(usuario).orElse(null);
        if (cart == null) {
            return new CartDTO(null, usuario.getId(), List.of());
        }
        return toDTO(cart);
    }

    @Transactional
    public CartDTO actualizarCart(Long idUsuario, CartDTO cartDTO) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Cart cart = cartRepository.findByUsuario(usuario).orElseGet(() -> Cart.builder().usuario(usuario).build());

        // Reemplazar items
        List<CartItem> nuevosItems = cartDTO.getItems().stream().map(itemDTO -> {
            Producto producto = productoRepository.findById(itemDTO.getIdProducto())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemDTO.getIdProducto()));
            return CartItem.builder()
                    .producto(producto)
                    .cantidad(itemDTO.getCantidad())
                    .selected(itemDTO.getSelected() != null ? itemDTO.getSelected() : Boolean.TRUE)
                    .build();
        }).collect(Collectors.toList());

        cart.setItems(nuevosItems);
        Cart saved = cartRepository.save(cart);
        return toDTO(saved);
    }

    private CartDTO toDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream().map(ci -> new CartItemDTO(
                ci.getProducto().getIdProducto(),
                ci.getCantidad(),
                ci.getSelected()
        )).collect(Collectors.toList());
        return new CartDTO(cart.getId(), cart.getUsuario().getId(), items);
    }
}

