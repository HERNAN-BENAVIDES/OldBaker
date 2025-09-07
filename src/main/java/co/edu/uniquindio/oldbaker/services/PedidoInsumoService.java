package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.model.PedidoInsumo;
import co.edu.uniquindio.oldbaker.repositories.PedidoInsumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PedidoInsumoService {

    private final PedidoInsumoRepository pedidoInsumoRepository;

    public PedidoInsumoService(PedidoInsumoRepository pedidoInsumoRepository) {
        this.pedidoInsumoRepository = pedidoInsumoRepository;
    }

    // Crear un nuevo pedido (lo hace el administrador)
    public PedidoInsumo crearPedido(PedidoInsumo pedido) {
        pedido.setEsPagable(false); // Por defecto aún no pagado
        return pedidoInsumoRepository.save(pedido);
    }

    // Obtener todos los pedidos
    public List<PedidoInsumo> listarPedidos() {
        return pedidoInsumoRepository.findAll();
    }

    // Buscar pedido por id
    public Optional<PedidoInsumo> buscarPorId(Long id) {
        return pedidoInsumoRepository.findById(id);
    }

    public PedidoInsumo actualizarPedido(Long id, PedidoInsumo pedidoActualizado) {
        return pedidoInsumoRepository.findById(id).map(pedido -> {
            pedido.setNombre(pedidoActualizado.getNombre());
            pedido.setDescripcion(pedidoActualizado.getDescripcion());
            pedido.setCostoTotal(pedidoActualizado.getCostoTotal());
            pedido.setFechaPedido(pedidoActualizado.getFechaPedido());
            pedido.setEsPagable(pedidoActualizado.getEsPagable());
            pedido.setPago(pedidoActualizado.getPago());
            return pedidoInsumoRepository.save(pedido);
        }).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    // Eliminar pedido
    public void eliminarPedido(Long id) {
        if (!pedidoInsumoRepository.existsById(id)) {
            throw new RuntimeException("Pedido no encontrado");
        }
        pedidoInsumoRepository.deleteById(id);
    }
}
