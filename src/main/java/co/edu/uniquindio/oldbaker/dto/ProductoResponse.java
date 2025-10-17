package co.edu.uniquindio.oldbaker.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductoResponse {
    private Long idProducto;
    private String nombre;
    private String descripcion;
    private Double costoUnitario;
    private int vidaUtilDias;
    private int pedidoMinimo;
    private String categoriaNombre;


    private List<RecetaDTO> receta;


}

