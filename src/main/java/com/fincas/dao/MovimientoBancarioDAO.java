package com.fincas.dao;

import com.fincas.db.DatabaseConnection;
import com.fincas.model.MovimientoBancario;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovimientoBancarioDAO {

    public List<MovimientoBancario> getAll() throws SQLException {
        List<MovimientoBancario> list = new ArrayList<>();
        String sql = "SELECT * FROM movimientos_bancarios ORDER BY fecha DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public MovimientoBancario getById(int id) throws SQLException {
        String sql = "SELECT * FROM movimientos_bancarios WHERE id = ?";
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

    public boolean insert(MovimientoBancario m) throws SQLException {
        String sqlInsert = "INSERT INTO movimientos_bancarios (banco_id, tipo, fecha, importe, categoria, inmueble_id, piso_local_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdateBanco = "";
        if ("INGRESO".equals(m.getTipo())) {
            sqlUpdateBanco = "UPDATE bancos SET saldo = saldo + ? WHERE id = ?";
        } else {
            sqlUpdateBanco = "UPDATE bancos SET saldo = saldo - ? WHERE id = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insertar movimiento
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, m.getBancoId());
                    ps.setString(2, m.getTipo());
                    ps.setDate(3, m.getFecha());
                    ps.setDouble(4, m.getImporte());
                    ps.setString(5, m.getCategoria());
                    if (m.getInmuebleId() != null) {
                        ps.setInt(6, m.getInmuebleId());
                    } else {
                        ps.setNull(6, Types.INTEGER);
                    }
                    if (m.getPisoLocalId() != null) {
                        ps.setInt(7, m.getPisoLocalId());
                    } else {
                        ps.setNull(7, Types.INTEGER);
                    }
                    
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                            if (rsKeys.next()) {
                                m.setId(rsKeys.getInt(1));
                            }
                        }
                    }
                }

                // 2. Modificar saldo bancario
                try (PreparedStatement psBanco = conn.prepareStatement(sqlUpdateBanco)) {
                    psBanco.setDouble(1, m.getImporte());
                    psBanco.setInt(2, m.getBancoId());
                    psBanco.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        MovimientoBancario m = getById(id);
        if (m == null) return false;

        String sqlDelete = "DELETE FROM movimientos_bancarios WHERE id = ?";
        String sqlUpdateBanco = "";
        // Revertimos la operación en el banco
        if ("INGRESO".equals(m.getTipo())) {
            sqlUpdateBanco = "UPDATE bancos SET saldo = saldo - ? WHERE id = ?";
        } else {
            sqlUpdateBanco = "UPDATE bancos SET saldo = saldo + ? WHERE id = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Eliminar movimiento
                try (PreparedStatement ps = conn.prepareStatement(sqlDelete)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                // 2. Revertir saldo bancario
                try (PreparedStatement psBanco = conn.prepareStatement(sqlUpdateBanco)) {
                    psBanco.setDouble(1, m.getImporte());
                    psBanco.setInt(2, m.getBancoId());
                    psBanco.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Retorna los movimientos detallados
    public List<Object[]> getMovimientosDetallados() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT m.id, b.nombre_banco, m.tipo, m.fecha, m.importe, m.categoria, " +
                     "inmG.direccion AS gasto_dir, inmG.tipo AS gasto_tipo, " +
                     "inmI.direccion AS ing_dir, inmI.tipo AS ing_tipo, inmI.planta, inmI.letra " +
                     "FROM movimientos_bancarios m " +
                     "JOIN bancos b ON m.banco_id = b.id " +
                     "LEFT JOIN inmuebles inmG ON m.inmueble_id = inmG.id " +
                     "LEFT JOIN inmuebles inmI ON m.piso_local_id = inmI.id " +
                     "ORDER BY m.fecha DESC, m.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Object[] row = new Object[7];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("nombre_banco");
                row[2] = rs.getString("tipo");
                row[3] = rs.getDate("fecha");
                row[4] = rs.getDouble("importe");
                row[5] = rs.getString("categoria");

                String asociacion = "-";
                if ("GASTO".equals(row[2]) && rs.getString("gasto_dir") != null) {
                    asociacion = rs.getString("gasto_tipo") + ": " + rs.getString("gasto_dir");
                } else if ("INGRESO".equals(row[2]) && rs.getString("ing_dir") != null) {
                    asociacion = rs.getString("ing_tipo") + ": " + rs.getString("ing_dir");
                    String p = rs.getString("planta");
                    String l = rs.getString("letra");
                    if (p != null && !p.isEmpty()) asociacion += " " + p + "º";
                    if (l != null && !l.isEmpty()) asociacion += " " + l;
                }
                row[6] = asociacion;
                list.add(row);
            }
        }
        return list;
    }

    // Reporte para la Declaración de la Renta
    // Agrupa ingresos y gastos por categoría en un rango de fechas
    public Map<String, Object> getResumenDeclaracionRenta(Date start, Date end) throws SQLException {
        Map<String, Object> report = new HashMap<>();
        List<Object[]> ingresos = new ArrayList<>();
        List<Object[]> gastos = new ArrayList<>();
        double totalIngresos = 0.0;
        double totalGastos = 0.0;

        String sql = "SELECT tipo, categoria, SUM(importe) AS total " +
                     "FROM movimientos_bancarios " +
                     "WHERE fecha BETWEEN ? AND ? " +
                     "GROUP BY tipo, categoria";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipo = rs.getString("tipo");
                    String cat = rs.getString("categoria");
                    double total = rs.getDouble("total");
                    if ("INGRESO".equals(tipo)) {
                        ingresos.add(new Object[]{cat, total});
                        totalIngresos += total;
                    } else {
                        gastos.add(new Object[]{cat, total});
                        totalGastos += total;
                    }
                }
            }
        }

        report.put("ingresos", ingresos);
        report.put("gastos", gastos);
        report.put("total_ingresos", totalIngresos);
        report.put("total_gastos", totalGastos);
        report.put("rendimiento_neto", totalIngresos - totalGastos);
        return report;
    }

    private MovimientoBancario mapResultSet(ResultSet rs) throws SQLException {
        int inmuebleVal = rs.getInt("inmueble_id");
        Integer inmuebleId = rs.wasNull() ? null : inmuebleVal;
        
        int pisoLocalVal = rs.getInt("piso_local_id");
        Integer pisoLocalId = rs.wasNull() ? null : pisoLocalVal;

        return new MovimientoBancario(
            rs.getInt("id"),
            rs.getInt("banco_id"),
            rs.getString("tipo"),
            rs.getDate("fecha"),
            rs.getDouble("importe"),
            rs.getString("categoria"),
            inmuebleId,
            pisoLocalId
        );
    }
}
