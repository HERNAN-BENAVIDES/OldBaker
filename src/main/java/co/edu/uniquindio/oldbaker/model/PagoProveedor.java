package co.edu.uniquindio.oldbaker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pagos_proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoProveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pago_proveedor_seq")
    @SequenceGenerator(name = "pago_proveedor_seq", sequenceName = "PAGO_PROVEEDOR_SEQ", allocationSize = 1)
    private Long idPago;
    private String descripcion;
    private Double monto;
    private LocalDate fechaPago;
    @OneToOne(mappedBy = "pago")
    @JsonIgnoreProperties({"pago", "detalles"})
    private PedidoInsumo pedido;

}