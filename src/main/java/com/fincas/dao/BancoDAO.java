package com.fincas.dao;

import com.fincas.db.DatabaseConnection;
import com.fincas.model.Banco;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BancoDAO {

    public List<Banco> getAll() throws SQLException {
        List<Banco> list = new ArrayList<>();
        String sql = "SELECT * FROM bancos";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public Banco getById(int id) throws SQLException {
        String sql = "SELECT * FROM bancos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean insert(Banco b) throws SQLException {
        String sql = "INSERT INTO bancos (nombre_banco, numero_cuenta, saldo) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getNombreBanco());
            ps.setString(2, b.getNumeroCuenta());
            ps.setDouble(3, b.getSaldo());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        b.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean update(Banco b) throws SQLException {
        String sql = "UPDATE bancos SET nombre_banco = ?, numero_cuenta = ?, saldo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getNombreBanco());
            ps.setString(2, b.getNumeroCuenta());
            ps.setDouble(3, b.getSaldo());
            ps.setInt(4, b.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM bancos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Banco mapResultSet(ResultSet rs) throws SQLException {
        return new Banco(
            rs.getInt("id"),
            rs.getString("nombre_banco"),
            rs.getString("numero_cuenta"),
            rs.getDouble("saldo")
        );
    }
}
