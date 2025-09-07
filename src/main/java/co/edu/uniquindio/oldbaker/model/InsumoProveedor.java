package co.edu.uniquindio.oldbaker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insumos_proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsumoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInsumo;

    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private LocalDate fechaVencimiento;
    private Integer cantidadDisponible;

    @ManyToOne
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @OneToOne(mappedBy = "insumoProveedor")
    private Insumo insumo;

    // Relación con detalle (un insumo puede estar en muchos detalles)
    @OneToMany(mappedBy = "insumo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleProveedorPedido> detalles = new ArrayList<>();
}

