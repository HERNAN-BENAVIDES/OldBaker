package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "insumos_proveedor")
@NoArgsConstructor
@AllArgsConstructor
public class InsumoProveedor {
    @Id
    private Long idInsumo;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private String fechaVencimiento;
    private Integer cantidadDisponible;
    @ManyToOne
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;
}
