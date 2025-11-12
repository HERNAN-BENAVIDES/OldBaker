package co.edu.uniquindio.oldbaker.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long idProducto;
    private Integer cantidad;
    private Boolean selected;
}

