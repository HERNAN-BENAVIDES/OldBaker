package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idProveedor;

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Número de teléfono inválido")
    private String telefono;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 34)
    private String numeroCuenta;
}
