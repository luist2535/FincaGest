package com.fincas.model;

public class Inmueble {
    private int id;
    private String tipo; // 'EDIFICIO', 'PISO', 'LOCAL'
    private String direccion;
    private String numero;
    private String codigoPostal;
    private String planta;
    private String letra;
    private Integer parentEdificioId;
    private String codigoRecibo;

    public Inmueble() {}

    public Inmueble(int id, String tipo, String direccion, String numero, String codigoPostal, String planta, String letra, Integer parentEdificioId, String codigoRecibo) {
        this.id = id;
        this.tipo = tipo;
        this.direccion = direccion;
        this.numero = numero;
        this.codigoPostal = codigoPostal;
        this.planta = planta;
        this.letra = letra;
        this.parentEdificioId = parentEdificioId;
        this.codigoRecibo = codigoRecibo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getPlanta() { return planta; }
    public void setPlanta(String planta) { this.planta = planta; }

    public String getLetra() { return letra; }
    public void setLetra(String letra) { this.letra = letra; }

    public Integer getParentEdificioId() { return parentEdificioId; }
    public void setParentEdificioId(Integer parentEdificioId) { this.parentEdificioId = parentEdificioId; }

    public String getCodigoRecibo() { return codigoRecibo; }
    public void setCodigoRecibo(String codigoRecibo) { this.codigoRecibo = codigoRecibo; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(tipo).append(": ").append(direccion).append(", ").append(numero);
        if (planta != null && !planta.isEmpty()) {
            sb.append(", Planta: ").append(planta);
        }
        if (letra != null && !letra.isEmpty()) {
            sb.append(", Letra/Pta: ").append(letra);
        }
        return sb.toString();
    }
}
