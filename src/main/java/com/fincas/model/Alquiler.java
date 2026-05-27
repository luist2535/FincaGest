package com.fincas.model;

import java.sql.Date;

public class Alquiler {
    private int id;
    private int inmuebleId;
    private int inquilinoId;
    private Date fechaInicio;
    private Date fechaFin;
    private boolean activo;

    public Alquiler() {}

    public Alquiler(int id, int inmuebleId, int inquilinoId, Date fechaInicio, Date fechaFin, boolean activo) {
        this.id = id;
        this.inmuebleId = inmuebleId;
        this.inquilinoId = inquilinoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInmuebleId() { return inmuebleId; }
    public void setInmuebleId(int inmuebleId) { this.inmuebleId = inmuebleId; }

    public int getInquilinoId() { return inquilinoId; }
    public void setInquilinoId(int inquilinoId) { this.inquilinoId = inquilinoId; }

    public Date getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Date fechaInicio) { this.fechaInicio = fechaInicio; }

    public Date getFechaFin() { return fechaFin; }
    public void setFechaFin(Date fechaFin) { this.fechaFin = fechaFin; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
