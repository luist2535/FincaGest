package com.fincas.dao;

import com.fincas.db.DatabaseConnection;
import com.fincas.model.Inquilino;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InquilinoDAO {

    public List<Inquilino> getAll() throws SQLException {
        List<Inquilino> list = new ArrayList<>();
        String sql = "SELECT * FROM inquilinos";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public Inquilino getById(int id) throws SQLException {
        String sql = "SELECT * FROM inquilinos WHERE id = ?";
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

    public Inquilino getByDni(String dni) throws SQLException {
        String sql = "SELECT * FROM inquilinos WHERE dni = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean insert(Inquilino i) throws SQLException {
        String sql = "INSERT INTO inquilinos (dni, nombre, edad, sexo, foto_path, metodo_garantia, avalador_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, i.getDni());
            ps.setString(2, i.getNombre());
            ps.setInt(3, i.getEdad());
            ps.setString(4, i.getSexo());
            ps.setString(5, i.getFotoPath());
            ps.setString(6, i.getMetodoGarantia());
            if (i.getAvaladorId() != null) {
                ps.setInt(7, i.getAvaladorId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        i.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean update(Inquilino i) throws SQLException {
        String sql = "UPDATE inquilinos SET dni = ?, nombre = ?, edad = ?, sexo = ?, foto_path = ?, metodo_garantia = ?, avalador_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, i.getDni());
            ps.setString(2, i.getNombre());
            ps.setInt(3, i.getEdad());
            ps.setString(4, i.getSexo());
            ps.setString(5, i.getFotoPath());
            ps.setString(6, i.getMetodoGarantia());
            if (i.getAvaladorId() != null) {
                ps.setInt(7, i.getAvaladorId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setInt(8, i.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM inquilinos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Listado de inquilinos ordenados por fecha de inicio de contrato (si tienen uno)
    public List<Object[]> getTenantsSortedByDate() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT i.dni, i.nombre, i.edad, i.sexo, i.metodo_garantia, a.fecha_inicio, inm.direccion, inm.planta, inm.letra " +
                     "FROM inquilinos i " +
                     "LEFT JOIN alquileres a ON i.id = a.inquilino_id AND a.activo = 1 " +
                     "LEFT JOIN inmuebles inm ON a.inmueble_id = inm.id " +
                     "ORDER BY a.fecha_inicio DESC, i.nombre ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Object[] row = new Object[7];
                row[0] = rs.getString("dni");
                row[1] = rs.getString("nombre");
                row[2] = rs.getInt("edad");
                row[3] = rs.getString("sexo");
                row[4] = rs.getString("metodo_garantia");
                row[5] = rs.getDate("fecha_inicio"); // Puede ser nulo
                String localInfo = rs.getString("direccion");
                if (localInfo != null) {
                    String planta = rs.getString("planta");
                    String letra = rs.getString("letra");
                    if (planta != null && !planta.isEmpty()) localInfo += " " + planta + "º";
                    if (letra != null && !letra.isEmpty()) localInfo += " " + letra;
                } else {
                    localInfo = "Sin Contrato Activo";
                }
                row[6] = localInfo;
                list.add(row);
            }
        }
        return list;
    }

    // Listado de inquilinos que han pagado o no en un determinado intervalo de tiempo
    public List<Object[]> getTenantsPaymentStatus(Date start, Date end, boolean paid) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT DISTINCT i.dni, i.nombre, r.fecha_emision, r.numero_recibo, r.cobrado, inm.direccion, inm.planta, inm.letra " +
                     "FROM inquilinos i " +
                     "JOIN alquileres a ON i.id = a.inquilino_id " +
                     "JOIN inmuebles inm ON a.inmueble_id = inm.id " +
                     "JOIN recibos r ON inm.id = r.inmueble_id " +
                     "WHERE r.fecha_emision BETWEEN ? AND ? AND r.cobrado = ? " +
                     "ORDER BY r.fecha_emision DESC, i.nombre ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            ps.setInt(3, paid ? 1 : 0);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[6];
                    row[0] = rs.getString("dni");
                    row[1] = rs.getString("nombre");
                    row[2] = rs.getDate("fecha_emision");
                    row[3] = rs.getString("numero_recibo");
                    String localInfo = rs.getString("direccion");
                    String planta = rs.getString("planta");
                    String letra = rs.getString("letra");
                    if (planta != null && !planta.isEmpty()) localInfo += " " + planta + "º";
                    if (letra != null && !letra.isEmpty()) localInfo += " " + letra;
                    row[4] = localInfo;
                    row[5] = rs.getBoolean("cobrado") ? "Cobrado" : "Pendiente";
                    list.add(row);
                }
            }
        }
        return list;
    }

    private Inquilino mapResultSet(ResultSet rs) throws SQLException {
        int avaladorVal = rs.getInt("avalador_id");
        Integer avaladorId = rs.wasNull() ? null : avaladorVal;
        return new Inquilino(
            rs.getInt("id"),
            rs.getString("dni"),
            rs.getString("nombre"),
            rs.getInt("edad"),
            rs.getString("sexo"),
            rs.getString("foto_path"),
            rs.getString("metodo_garantia"),
            avaladorId
        );
    }
}
