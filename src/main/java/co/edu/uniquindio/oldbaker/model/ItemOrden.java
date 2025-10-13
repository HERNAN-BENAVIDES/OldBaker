package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;

@Data
@Entity
public class ItemOrden{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_orden_seq")
    @SequenceGenerator(name = "item_orden_seq", sequenceName = "ITEM_ORDEN_SEQ", allocationSize = 1)
    private Long id;

}
