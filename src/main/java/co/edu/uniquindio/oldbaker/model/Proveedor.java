package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proveedor_seq")
    @SequenceGenerator(name = "proveedor_seq", sequenceName = "PROVEEDOR_SEQ", allocationSize = 1)
    private Long idProveedor;
    private String nombre;
    private String telefono;
    private String email;
    private String numeroCuenta;
}