package com.fincas.model;

import java.sql.Date;

public class Recibo {
    private int id;
    private int inmuebleId;
    private String numeroRecibo;
    private Date fechaEmision;
    private double renta;
    private double agua;
    private double luz;
    private double ipc;
    private double porteria;
    private double iva;
    private double otrosConceptos;
    private String descripcionOtros;
    private boolean cobrado;

    public Recibo() {}

    public Recibo(int id, int inmuebleId, String numeroRecibo, Date fechaEmision, double renta, double agua, double luz, double ipc, double porteria, double iva, double otrosConceptos, String descripcionOtros, boolean cobrado) {
        this.id = id;
        this.inmuebleId = inmuebleId;
        this.numeroRecibo = numeroRecibo;
        this.fechaEmision = fechaEmision;
        this.renta = renta;
        this.agua = agua;
        this.luz = luz;
        this.ipc = ipc;
        this.porteria = porteria;
        this.iva = iva;
        this.otrosConceptos = otrosConceptos;
        this.descripcionOtros = descripcionOtros;
        this.cobrado = cobrado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInmuebleId() { return inmuebleId; }
    public void setInmuebleId(int inmuebleId) { this.inmuebleId = inmuebleId; }

    public String getNumeroRecibo() { return numeroRecibo; }
    public void setNumeroRecibo(String numeroRecibo) { this.numeroRecibo = numeroRecibo; }

    public Date getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(Date fechaEmision) { this.fechaEmision = fechaEmision; }

    public double getRenta() { return renta; }
    public void setRenta(double renta) { this.renta = renta; }

    public double getAgua() { return agua; }
    public void setAgua(double agua) { this.agua = agua; }

    public double getLuz() { return luz; }
    public void setLuz(double luz) { this.luz = luz; }

    public double getIpc() { return ipc; }
    public void setIpc(double ipc) { this.ipc = ipc; }

    public double getPorteria() { return porteria; }
    public void setPorteria(double porteria) { this.porteria = porteria; }

    public double getIva() { return iva; }
    public void setIva(double iva) { this.iva = iva; }

    public double getOtrosConceptos() { return otrosConceptos; }
    public void setOtrosConceptos(double otrosConceptos) { this.otrosConceptos = otrosConceptos; }

    public String getDescripcionOtros() { return descripcionOtros; }
    public void setDescripcionOtros(String descripcionOtros) { this.descripcionOtros = descripcionOtros; }

    public boolean isCobrado() { return cobrado; }
    public void setCobrado(boolean cobrado) { this.cobrado = cobrado; }

    public double getTotal() {
        return renta + agua + luz + ipc + porteria + iva + otrosConceptos;
    }
}
