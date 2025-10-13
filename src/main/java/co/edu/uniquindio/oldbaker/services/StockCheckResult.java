package co.edu.uniquindio.oldbaker.services;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockCheckResult {
    private boolean valid;
    private String message;
}

