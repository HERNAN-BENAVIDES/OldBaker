package co.edu.uniquindio.oldbaker.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutItemDTO {

    private Long productId;
    private Integer quantity;

}
