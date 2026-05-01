package com.catolica.Artepax.inventario.producto.dto;

public class StockProductoResponseDTO {private Long idProducto;
    private String nombre;
    private Integer cantidadTotal;
    private Integer cantidadReservada;
    private Integer cantidadDisponible;
    private Integer stockMinimo;
    private Boolean stockBajoAlerta;

    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getCantidadTotal() { return cantidadTotal; }
    public void setCantidadTotal(Integer cantidadTotal) { this.cantidadTotal = cantidadTotal; }

    public Integer getCantidadReservada() { return cantidadReservada; }
    public void setCantidadReservada(Integer cantidadReservada) { this.cantidadReservada = cantidadReservada; }

    public Integer getCantidadDisponible() { return cantidadDisponible; }
    public void setCantidadDisponible(Integer cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public Boolean getStockBajoAlerta() { return stockBajoAlerta; }
    public void setStockBajoAlerta(Boolean stockBajoAlerta) { this.stockBajoAlerta = stockBajoAlerta; }
}