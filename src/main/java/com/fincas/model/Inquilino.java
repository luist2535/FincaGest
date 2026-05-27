package com.fincas.model;

public class Inquilino {
    private int id;
    private String dni;
    private String nombre;
    private int edad;
    private String sexo;
    private String fotoPath;
    private String metodoGarantia; // 'NOMINA', 'AVAL_BANCARIO', 'CONTRATO_TRABAJO', 'AVALADO_POR_OTRO'
    private Integer avaladorId;

    public Inquilino() {}

    public Inquilino(int id, String dni, String nombre, int edad, String sexo, String fotoPath, String metodoGarantia, Integer avaladorId) {
        this.id = id;
        this.dni = dni;
        this.nombre = nombre;
        this.edad = edad;
        this.sexo = sexo;
        this.fotoPath = fotoPath;
        this.metodoGarantia = metodoGarantia;
        this.avaladorId = avaladorId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getFotoPath() { return fotoPath; }
    public void setFotoPath(String fotoPath) { this.fotoPath = fotoPath; }

    public String getMetodoGarantia() { return metodoGarantia; }
    public void setMetodoGarantia(String metodoGarantia) { this.metodoGarantia = metodoGarantia; }

    public Integer getAvaladorId() { return avaladorId; }
    public void setAvaladorId(Integer avaladorId) { this.avaladorId = avaladorId; }

    @Override
    public String toString() {
        return nombre + " (" + dni + ")";
    }
}
