package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;

@Data
@Entity
public class OrdenCompra{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orden_compra_seq")
    @SequenceGenerator(name = "orden_compra_seq", sequenceName = "ORDEN_COMPRA_SEQ", allocationSize = 1)
    private Long id;

}
