package com.fincas.dao;

import com.fincas.db.DatabaseConnection;
import com.fincas.model.Alquiler;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlquilerDAO {

    public List<Alquiler> getAll() throws SQLException {
        List<Alquiler> list = new ArrayList<>();
        String sql = "SELECT * FROM alquileres";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public Alquiler getById(int id) throws SQLException {
        String sql = "SELECT * FROM alquileres WHERE id = ?";
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

    public Alquiler getAlquilerActivo(int inmuebleId) throws SQLException {
        String sql = "SELECT * FROM alquileres WHERE inmueble_id = ? AND activo = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inmuebleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean alquilar(int inmuebleId, int inquilinoId, Date fechaInicio) throws SQLException {
        // Primero nos aseguramos de que no esté alquilado
        Alquiler activo = getAlquilerActivo(inmuebleId);
        if (activo != null) {
            throw new SQLException("El inmueble ya se encuentra alquilado actualmente.");
        }

        String sql = "INSERT INTO alquileres (inmueble_id, inquilino_id, fecha_inicio, activo) VALUES (?, ?, ?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inmuebleId);
            ps.setInt(2, inquilinoId);
            ps.setDate(3, fechaInicio);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean desalquilar(int inmuebleId, Date fechaFin) throws SQLException {
        Alquiler activo = getAlquilerActivo(inmuebleId);
        if (activo == null) {
            throw new SQLException("El inmueble no está alquilado actualmente.");
        }

        String sql = "UPDATE alquileres SET activo = 0, fecha_fin = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, fechaFin);
            ps.setInt(2, activo.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public List<Object[]> getAlquileresDetallados() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT a.id, inm.id AS inmueble_id, inm.tipo, inm.direccion, inm.planta, inm.letra, " +
                     "i.nombre AS inquilino_nombre, i.dni, a.fecha_inicio, a.activo " +
                     "FROM alquileres a " +
                     "JOIN inmuebles inm ON a.inmueble_id = inm.id " +
                     "JOIN inquilinos i ON a.inquilino_id = i.id " +
                     "ORDER BY a.activo DESC, a.fecha_inicio DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Object[] row = new Object[7];
                row[0] = rs.getInt("id");
                
                String desc = rs.getString("tipo") + ": " + rs.getString("direccion");
                String p = rs.getString("planta");
                String l = rs.getString("letra");
                if (p != null && !p.isEmpty()) desc += " " + p + "º";
                if (l != null && !l.isEmpty()) desc += " " + l;
                row[1] = desc;

                row[2] = rs.getString("inquilino_nombre") + " (" + rs.getString("dni") + ")";
                row[3] = rs.getDate("fecha_inicio");
                row[4] = rs.getInt("activo") == 1 ? "Activo" : "Finalizado";
                row[5] = rs.getInt("inmueble_id");
                row[6] = rs.getString("dni"); // DNI del inquilino
                list.add(row);
            }
        }
        return list;
    }

    private Alquiler mapResultSet(ResultSet rs) throws SQLException {
        return new Alquiler(
            rs.getInt("id"),
            rs.getInt("inmueble_id"),
            rs.getInt("inquilino_id"),
            rs.getDate("fecha_inicio"),
            rs.getDate("fecha_fin"),
            rs.getBoolean("activo")
        );
    }
}
