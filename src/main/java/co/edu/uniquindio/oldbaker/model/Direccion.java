package co.edu.uniquindio.oldbaker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Entity
@Table(name = "direcciones")
public class Direccion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "direccion_seq")
    @SequenceGenerator(name = "direccion_seq", sequenceName = "DIRECCION_SEQ", allocationSize = 1)
    private Long id;
    private String ciudad;
    private String barrio;
    private String carrera;
    private String calle;
    private String numero;
    private String numeroTelefono;
    @ManyToOne
    @JsonIgnoreProperties({"direcciones", "password", "ordenes"})
    private Usuario idCliente;

}
