package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.payment.CheckoutItemDTO;
import co.edu.uniquindio.oldbaker.dto.payment.CheckoutRequestDTO;
import co.edu.uniquindio.oldbaker.model.Insumo;
import co.edu.uniquindio.oldbaker.model.Receta;
import co.edu.uniquindio.oldbaker.repositories.InsumoRepository;
import co.edu.uniquindio.oldbaker.repositories.RecetaRepository;
import lombok.RequiredArgsConstructor;
 import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockValidationService {

    private static final Logger logger = LoggerFactory.getLogger(StockValidationService.class);

    private final RecetaRepository recetaRepository;
    private final InsumoRepository insumoRepository;

    /**
     * Valida que para los items de checkout exista receta y haya stock suficiente de insumos.
     * Si no hay stock suficiente para algún item, devuelve valid=false y un mensaje indicando
     * qué producto no se puede preparar en la cantidad solicitada y cuántas unidades sí se pueden preparar.
     */
    public StockCheckResult checkAvailability(CheckoutRequestDTO request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return new StockCheckResult(true, null);
        }

        // Cargar todas las recetas necesarias y construir conjunto de insumos a consultar
        // Primero, recolectar todos los productos del pedido para obtener recetas
        Map<Long, List<Receta>> recetasPorProducto = new HashMap<>();
        for (CheckoutItemDTO item : request.getItems()) {
            if (item == null || item.getProductId() == null) continue;
            Long productId = item.getProductId();
            if (!recetasPorProducto.containsKey(productId)) {
                List<Receta> recetaItems = recetaRepository.findByProducto_IdProducto(productId);
                recetasPorProducto.put(productId, recetaItems);
            }
        }

        // Verificar que todos los productos tengan receta
        for (Map.Entry<Long, List<Receta>> e : recetasPorProducto.entrySet()) {
            Long pid = e.getKey();
            List<Receta> recs = e.getValue();
            if (recs == null || recs.isEmpty()) {
                return new StockCheckResult(false, "Producto " + pid + " no tiene receta definida.");
            }
        }

        // Construir mapa de insumoId -> disponible (double), cargando solo los insumos implicados
        Set<Long> insumoIds = recetasPorProducto.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getInsumo() != null)
                .map(r -> r.getInsumo().getIdInsumo())
                .collect(Collectors.toSet());

        Map<Long, Double> availableMap = new HashMap<>();
        if (!insumoIds.isEmpty()) {
            List<Insumo> insumos = insumoRepository.findAllById(insumoIds);
            for (Insumo ins : insumos) {
                if (ins == null) continue;
                double cant = ins.getCantidadActual() != null ? ins.getCantidadActual() : 0.0;
                availableMap.put(ins.getIdInsumo(), cant);
            }
        }

        StringBuilder mensaje = new StringBuilder();
        boolean allOk = true;

        // Iterar por cada item en el pedido y reservar insumos en orden
        for (CheckoutItemDTO item : request.getItems()) {
            if (item == null) continue;
            Long productId = item.getProductId();
            int requestedQty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (productId == null || requestedQty <= 0) continue;

            List<Receta> recetaItems = recetasPorProducto.get(productId);
            if (recetaItems == null || recetaItems.isEmpty()) {
                // ya verificado antes, pero por seguridad
                return new StockCheckResult(false, "Producto " + productId + " no tiene receta definida.");
            }

            // Calcular la máxima cantidad de este producto que se puede preparar con el stock disponible actual
            double maxPossible = Double.POSITIVE_INFINITY;
            for (Receta r : recetaItems) {
                if (r == null || r.getInsumo() == null) continue;
                Long insumoId = r.getInsumo().getIdInsumo();
                // convertir de forma segura la cantidad por producto (soporta primitives y wrappers)
                Number cantidadObj = r.getCantidadInsumo();
                double needPerProduct = cantidadObj != null ? cantidadObj.doubleValue() : 0.0;
                if (needPerProduct <= 0) continue; // si no consume este insumo, no limita
                double avail = availableMap.getOrDefault(insumoId, 0.0);
                double possibleForThisInsumo = Math.floor(avail / needPerProduct);
                if (possibleForThisInsumo < maxPossible) maxPossible = possibleForThisInsumo;
            }

            if (maxPossible == Double.POSITIVE_INFINITY) {
                // No insumos limitantes (receta vacía o solo insumos con cantidad 0)
                maxPossible = Integer.MAX_VALUE;
            }

            int maxUnits = (int) Math.max(0, Math.floor(maxPossible));

            if (maxUnits >= requestedQty) {
                // Reservar los insumos necesarios para la cantidad solicitada
                for (Receta r : recetaItems) {
                    if (r == null || r.getInsumo() == null) continue;
                    Long insumoId = r.getInsumo().getIdInsumo();
                    Number cantidadObj = r.getCantidadInsumo();
                    double needPerProduct = cantidadObj != null ? cantidadObj.doubleValue() : 0.0;
                    double totalNeed = needPerProduct * requestedQty;
                    double remaining = availableMap.getOrDefault(insumoId, 0.0) - totalNeed;
                    availableMap.put(insumoId, Math.max(0.0, remaining));
                }
            } else {
                // No se puede preparar la cantidad solicitada; indicar cuántas se sí pueden preparar
                allOk = false;
                String nombreProducto = "producto id=" + productId;
                // intentar obtener nombre desde la receta (producto relacionado)
                if (!recetaItems.isEmpty() && recetaItems.get(0).getProducto() != null
                        && recetaItems.get(0).getProducto().getNombre() != null) {
                    nombreProducto = recetaItems.get(0).getProducto().getNombre();
                }
                mensaje.append("Producto '").append(nombreProducto).append("' no se puede preparar en la cantidad solicitada (")
                        .append(requestedQty).append("). Se puede preparar hasta ")
                        .append(maxUnits).append(" unidades. ");

                // Reservar los insumos para las unidades que sí se pueden preparar (maxUnits)
                if (maxUnits > 0) {
                    for (Receta r : recetaItems) {
                        if (r == null || r.getInsumo() == null) continue;
                        Long insumoId = r.getInsumo().getIdInsumo();
                        Number cantidadObj = r.getCantidadInsumo();
                        double needPerProduct = cantidadObj != null ? cantidadObj.doubleValue() : 0.0;
                        double totalNeed = needPerProduct * maxUnits;
                        double remaining = availableMap.getOrDefault(insumoId, 0.0) - totalNeed;
                        availableMap.put(insumoId, Math.max(0.0, remaining));
                    }
                }
            }
        }

        if (!allOk) {
            logger.warn("Validación de stock falló: {}", mensaje);
            return new StockCheckResult(false, mensaje.toString());
        }

        return new StockCheckResult(true, null);
    }
}
