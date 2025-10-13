package co.edu.uniquindio.oldbaker.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO {
    private String initPoint;
    private String preferenceId;

}

