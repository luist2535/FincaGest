package com.fincas.dao;

import com.fincas.db.DatabaseConnection;
import com.fincas.model.Inmueble;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InmuebleDAO {

    public List<Inmueble> getAll() throws SQLException {
        List<Inmueble> list = new ArrayList<>();
        String sql = "SELECT * FROM inmuebles";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public Inmueble getById(int id) throws SQLException {
        String sql = "SELECT * FROM inmuebles WHERE id = ?";
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

    public List<Inmueble> getPisosYLocalesDeEdificio(int edificioId) throws SQLException {
        List<Inmueble> list = new ArrayList<>();
        String sql = "SELECT * FROM inmuebles WHERE parent_edificio_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, edificioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    // Retorna los inmuebles que NO están alquilados actualmente (no tienen contrato activo)
    public List<Inmueble> getDisponiblesParaAlquiler() throws SQLException {
        List<Inmueble> list = new ArrayList<>();
        // Un inmueble está disponible si no tiene un alquiler activo y además no es un edificio que contenga pisos alquilados?
        // El enunciado dice: "Cualquier persona... puede alquilar el edificio completo o alguno de los pisos o locales que no estén ya alquilados"
        String sql = "SELECT * FROM inmuebles i WHERE i.id NOT IN (SELECT inmueble_id FROM alquileres WHERE activo = 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public boolean insert(Inmueble i) throws SQLException {
        String sql = "INSERT INTO inmuebles (tipo, direccion, numero, codigo_postal, planta, letra, parent_edificio_id, codigo_recibo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, i.getTipo());
            ps.setString(2, i.getDireccion());
            ps.setString(3, i.getNumero());
            ps.setString(4, i.getCodigoPostal());
            ps.setString(5, i.getPlanta());
            ps.setString(6, i.getLetra());
            if (i.getParentEdificioId() != null) {
                ps.setInt(7, i.getParentEdificioId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, i.getCodigoRecibo());
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

    public boolean update(Inmueble i) throws SQLException {
        String sql = "UPDATE inmuebles SET tipo = ?, direccion = ?, numero = ?, codigo_postal = ?, planta = ?, letra = ?, parent_edificio_id = ?, codigo_recibo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, i.getTipo());
            ps.setString(2, i.getDireccion());
            ps.setString(3, i.getNumero());
            ps.setString(4, i.getCodigoPostal());
            ps.setString(5, i.getPlanta());
            ps.setString(6, i.getLetra());
            if (i.getParentEdificioId() != null) {
                ps.setInt(7, i.getParentEdificioId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, i.getCodigoRecibo());
            ps.setInt(9, i.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM inmuebles WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Inmueble mapResultSet(ResultSet rs) throws SQLException {
        int parentVal = rs.getInt("parent_edificio_id");
        Integer parentEdificioId = rs.wasNull() ? null : parentVal;
        return new Inmueble(
            rs.getInt("id"),
            rs.getString("tipo"),
            rs.getString("direccion"),
            rs.getString("numero"),
            rs.getString("codigo_postal"),
            rs.getString("planta"),
            rs.getString("letra"),
            parentEdificioId,
            rs.getString("codigo_recibo")
        );
    }
}
