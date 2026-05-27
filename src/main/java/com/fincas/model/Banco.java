package com.fincas.model;

public class Banco {
    private int id;
    private String nombreBanco;
    private String numeroCuenta;
    private double saldo;

    public Banco() {}

    public Banco(int id, String nombreBanco, String numeroCuenta, double saldo) {
        this.id = id;
        this.nombreBanco = nombreBanco;
        this.numeroCuenta = numeroCuenta;
        this.saldo = saldo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreBanco() { return nombreBanco; }
    public void setNombreBanco(String nombreBanco) { this.nombreBanco = nombreBanco; }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    @Override
    public String toString() {
        return nombreBanco + " - " + numeroCuenta + " (Saldo: " + String.format("%.2f", saldo) + "€)";
    }
}
