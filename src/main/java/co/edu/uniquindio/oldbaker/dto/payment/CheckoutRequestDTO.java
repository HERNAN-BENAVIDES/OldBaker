package co.edu.uniquindio.oldbaker.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDTO {

    private List<CheckoutItemDTO> items;
    private String payerEmail;

}
