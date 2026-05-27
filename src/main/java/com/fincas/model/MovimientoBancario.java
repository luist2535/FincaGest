package com.fincas.model;

import java.sql.Date;

public class MovimientoBancario {
    private int id;
    private int bancoId;
    private String tipo; // 'INGRESO' o 'GASTO'
    private Date fecha;
    private double importe;
    private String categoria; // 'REPARACION', 'LIMPIEZA', 'RECIBO_ALQUILER', 'SUELDO', 'OTROS_GASTOS', 'OTROS_INGRESOS'
    private Integer inmuebleId; // Para gastos (asociado a un edificio o inmueble general)
    private Integer pisoLocalId; // Para ingresos (asociado a un piso/local específico)

    public MovimientoBancario() {}

    public MovimientoBancario(int id, int bancoId, String tipo, Date fecha, double importe, String categoria, Integer inmuebleId, Integer pisoLocalId) {
        this.id = id;
        this.bancoId = bancoId;
        this.tipo = tipo;
        this.fecha = fecha;
        this.importe = importe;
        this.categoria = categoria;
        this.inmuebleId = inmuebleId;
        this.pisoLocalId = pisoLocalId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBancoId() { return bancoId; }
    public void setBancoId(int bancoId) { this.bancoId = bancoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public double getImporte() { return importe; }
    public void setImporte(double importe) { this.importe = importe; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getInmuebleId() { return inmuebleId; }
    public void setInmuebleId(Integer inmuebleId) { this.inmuebleId = inmuebleId; }

    public Integer getPisoLocalId() { return pisoLocalId; }
    public void setPisoLocalId(Integer pisoLocalId) { this.pisoLocalId = pisoLocalId; }
}
