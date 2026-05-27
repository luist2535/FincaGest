package com.fincas.dao;

import com.fincas.db.DatabaseConnection;
import com.fincas.model.Recibo;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReciboDAO {

    public List<Recibo> getAll() throws SQLException {
        List<Recibo> list = new ArrayList<>();
        String sql = "SELECT * FROM recibos ORDER BY fecha_emision DESC, numero_recibo ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public Recibo getById(int id) throws SQLException {
        String sql = "SELECT * FROM recibos WHERE id = ?";
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

    public boolean insert(Recibo r) throws SQLException {
        String sql = "INSERT INTO recibos (inmueble_id, numero_recibo, fecha_emision, renta, agua, luz, ipc, porteria, iva, otros_conceptos, descripcion_otros, cobrado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getInmuebleId());
            ps.setString(2, r.getNumeroRecibo());
            ps.setDate(3, r.getFechaEmision());
            ps.setDouble(4, r.getRenta());
            ps.setDouble(5, r.getAgua());
            ps.setDouble(6, r.getLuz());
            ps.setDouble(7, r.getIpc());
            ps.setDouble(8, r.getPorteria());
            ps.setDouble(9, r.getIva());
            ps.setDouble(10, r.getOtrosConceptos());
            ps.setString(11, r.getDescripcionOtros());
            ps.setInt(12, r.isCobrado() ? 1 : 0);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        r.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean update(Recibo r) throws SQLException {
        String sql = "UPDATE recibos SET inmueble_id = ?, numero_recibo = ?, fecha_emision = ?, renta = ?, agua = ?, luz = ?, ipc = ?, porteria = ?, iva = ?, otros_conceptos = ?, descripcion_otros = ?, cobrado = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getInmuebleId());
            ps.setString(2, r.getNumeroRecibo());
            ps.setDate(3, r.getFechaEmision());
            ps.setDouble(4, r.getRenta());
            ps.setDouble(5, r.getAgua());
            ps.setDouble(6, r.getLuz());
            ps.setDouble(7, r.getIpc());
            ps.setDouble(8, r.getPorteria());
            ps.setDouble(9, r.getIva());
            ps.setDouble(10, r.getOtrosConceptos());
            ps.setString(11, r.getDescripcionOtros());
            ps.setInt(12, r.isCobrado() ? 1 : 0);
            ps.setInt(13, r.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM recibos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Generar recibos duplicando los del mes anterior o con valores por defecto
    public int generarRecibosMesAnterior(Date nuevaFecha) throws SQLException {
        // Encontrar todos los alquileres activos
        String sqlAlquileres = "SELECT a.inmueble_id, i.codigo_recibo FROM alquileres a JOIN inmuebles i ON a.inmueble_id = i.id WHERE a.activo = 1 AND i.tipo IN ('PISO', 'LOCAL')";
        int count = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Integer> inmuebleIds = new ArrayList<>();
                List<String> codigosRecibos = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sqlAlquileres)) {
                    while (rs.next()) {
                        inmuebleIds.add(rs.getInt("inmueble_id"));
                        codigosRecibos.add(rs.getString("codigo_recibo"));
                    }
                }

                String sqlUltimoRecibo = "SELECT * FROM recibos WHERE inmueble_id = ? ORDER BY fecha_emision DESC LIMIT 1";
                String sqlInsert = "INSERT INTO recibos (inmueble_id, numero_recibo, fecha_emision, renta, agua, luz, ipc, porteria, iva, otros_conceptos, descripcion_otros, cobrado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";

                try (PreparedStatement psUltimo = conn.prepareStatement(sqlUltimoRecibo);
                     PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                    
                    for (int i = 0; i < inmuebleIds.size(); i++) {
                        int inmuebleId = inmuebleIds.get(i);
                        String codRecibo = codigosRecibos.get(i);
                        if (codRecibo == null || codRecibo.isEmpty()) {
                            codRecibo = "REC-" + inmuebleId;
                        }

                        // Verificar si ya existe un recibo de este mes para evitar duplicados
                        if (existeReciboEnMes(conn, inmuebleId, nuevaFecha)) {
                            continue;
                        }

                        psUltimo.setInt(1, inmuebleId);
                        try (ResultSet rsUltimo = psUltimo.executeQuery()) {
                            if (rsUltimo.next()) {
                                // Clonar del mes anterior
                                psInsert.setInt(1, inmuebleId);
                                psInsert.setString(2, codRecibo);
                                psInsert.setDate(3, nuevaFecha);
                                psInsert.setDouble(4, rsUltimo.getDouble("renta"));
                                psInsert.setDouble(5, rsUltimo.getDouble("agua"));
                                psInsert.setDouble(6, rsUltimo.getDouble("luz"));
                                psInsert.setDouble(7, rsUltimo.getDouble("ipc"));
                                psInsert.setDouble(8, rsUltimo.getDouble("porteria"));
                                psInsert.setDouble(9, rsUltimo.getDouble("iva"));
                                psInsert.setDouble(10, rsUltimo.getDouble("otros_conceptos"));
                                psInsert.setString(11, rsUltimo.getString("descripcion_otros"));
                            } else {
                                // Valores por defecto si no hay recibo anterior
                                double rentaDefault = 500.00;
                                double ivaDefault = rentaDefault * 0.21; // 21% IVA estándar
                                psInsert.setInt(1, inmuebleId);
                                psInsert.setString(2, codRecibo);
                                psInsert.setDate(3, nuevaFecha);
                                psInsert.setDouble(4, rentaDefault);
                                psInsert.setDouble(5, 0.0);
                                psInsert.setDouble(6, 0.0);
                                psInsert.setDouble(7, 0.0);
                                psInsert.setDouble(8, 0.0);
                                psInsert.setDouble(9, ivaDefault);
                                psInsert.setDouble(10, 0.0);
                                psInsert.setString(11, "");
                            }
                            psInsert.executeUpdate();
                            count++;
                        }
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return count;
    }

    private boolean existeReciboEnMes(Connection conn, int inmuebleId, Date fecha) throws SQLException {
        String sql = "SELECT id FROM recibos WHERE inmueble_id = ? AND MONTH(fecha_emision) = MONTH(?) AND YEAR(fecha_emision) = YEAR(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inmuebleId);
            ps.setDate(2, fecha);
            ps.setDate(3, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Inicializar un concepto de todos los recibos en una fecha a una cantidad
    public int inicializarConcepto(String concepto, double cantidad, int mes, int anio) throws SQLException {
        // Conceptos válidos: 'renta', 'agua', 'luz', 'ipc', 'porteria', 'iva', 'otros_conceptos'
        String sql = "UPDATE recibos SET " + concepto + " = ? WHERE MONTH(fecha_emision) = ? AND YEAR(fecha_emision) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, cantidad);
            ps.setInt(2, mes);
            ps.setInt(3, anio);
            return ps.executeUpdate();
        }
    }

    // Registrar cobro de recibo asociándolo a una cuenta de banco
    public boolean cobrarRecibo(int reciboId, int bancoId) throws SQLException {
        Recibo r = getById(reciboId);
        if (r == null) {
            throw new SQLException("El recibo no existe.");
        }
        if (r.isCobrado()) {
            throw new SQLException("El recibo ya ha sido cobrado previamente.");
        }

        double total = r.getTotal();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Actualizar recibo como cobrado
                String sqlUpdateRecibo = "UPDATE recibos SET cobrado = 1 WHERE id = ?";
                try (PreparedStatement ps1 = conn.prepareStatement(sqlUpdateRecibo)) {
                    ps1.setInt(1, reciboId);
                    ps1.executeUpdate();
                }

                // 2. Incrementar saldo en el banco
                String sqlUpdateBanco = "UPDATE bancos SET saldo = saldo + ? WHERE id = ?";
                try (PreparedStatement ps2 = conn.prepareStatement(sqlUpdateBanco)) {
                    ps2.setDouble(1, total);
                    ps2.setInt(2, bancoId);
                    ps2.executeUpdate();
                }

                // 3. Crear movimiento bancario
                String sqlInsertMov = "INSERT INTO movimientos_bancarios (banco_id, tipo, fecha, importe, categoria, piso_local_id) VALUES (?, 'INGRESO', ?, ?, 'RECIBO_ALQUILER', ?)";
                try (PreparedStatement ps3 = conn.prepareStatement(sqlInsertMov)) {
                    ps3.setInt(1, bancoId);
                    ps3.setDate(2, new Date(System.currentTimeMillis()));
                    ps3.setDouble(3, total);
                    ps3.setInt(4, r.getInmuebleId());
                    ps3.executeUpdate();
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

    // Listado de recibos detallados con dirección del inmueble
    public List<Object[]> getRecibosDetallados() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT r.id, r.numero_recibo, r.fecha_emision, r.cobrado, " +
                     "inm.tipo, inm.direccion, inm.planta, inm.letra, " +
                     "r.renta, r.agua, r.luz, r.ipc, r.porteria, r.iva, r.otros_conceptos, r.descripcion_otros " +
                     "FROM recibos r " +
                     "JOIN inmuebles inm ON r.inmueble_id = inm.id " +
                     "ORDER BY r.fecha_emision DESC, r.numero_recibo ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Object[] row = new Object[16];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("numero_recibo");
                row[2] = rs.getDate("fecha_emision");
                row[3] = rs.getBoolean("cobrado") ? "Cobrado" : "Pendiente";
                
                String desc = rs.getString("tipo") + ": " + rs.getString("direccion");
                String p = rs.getString("planta");
                String l = rs.getString("letra");
                if (p != null && !p.isEmpty()) desc += " " + p + "º";
                if (l != null && !l.isEmpty()) desc += " " + l;
                row[4] = desc;
                
                row[5] = rs.getDouble("renta");
                row[6] = rs.getDouble("agua");
                row[7] = rs.getDouble("luz");
                row[8] = rs.getDouble("ipc");
                row[9] = rs.getDouble("porteria");
                row[10] = rs.getDouble("iva");
                row[11] = rs.getDouble("otros_conceptos");
                row[12] = rs.getString("descripcion_otros");
                
                double total = rs.getDouble("renta") + rs.getDouble("agua") + rs.getDouble("luz") +
                               rs.getDouble("ipc") + rs.getDouble("porteria") + rs.getDouble("iva") +
                               rs.getDouble("otros_conceptos");
                row[13] = total;
                row[14] = rs.getInt("id"); // Para operaciones
                list.add(row);
            }
        }
        return list;
    }

    // Listado de todos los recibos pendientes de cobro en un determinado intervalo de tiempo
    public List<Object[]> getRecibosPendientesIntervalo(Date start, Date end) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT r.id, r.numero_recibo, r.fecha_emision, inm.tipo, inm.direccion, inm.planta, inm.letra, " +
                     "r.renta, r.agua, r.luz, r.ipc, r.porteria, r.iva, r.otros_conceptos " +
                     "FROM recibos r " +
                     "JOIN inmuebles inm ON r.inmueble_id = inm.id " +
                     "WHERE r.cobrado = 0 AND r.fecha_emision BETWEEN ? AND ? " +
                     "ORDER BY r.fecha_emision ASC, r.numero_recibo ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[9];
                    row[0] = rs.getInt("id");
                    row[1] = rs.getString("numero_recibo");
                    row[2] = rs.getDate("fecha_emision");
                    
                    String desc = rs.getString("tipo") + ": " + rs.getString("direccion");
                    String p = rs.getString("planta");
                    String l = rs.getString("letra");
                    if (p != null && !p.isEmpty()) desc += " " + p + "º";
                    if (l != null && !l.isEmpty()) desc += " " + l;
                    row[3] = desc;

                    double total = rs.getDouble("renta") + rs.getDouble("agua") + rs.getDouble("luz") +
                                   rs.getDouble("ipc") + rs.getDouble("porteria") + rs.getDouble("iva") +
                                   rs.getDouble("otros_conceptos");
                    row[4] = total;
                    list.add(row);
                }
            }
        }
        return list;
    }

    private Recibo mapResultSet(ResultSet rs) throws SQLException {
        return new Recibo(
            rs.getInt("id"),
            rs.getInt("inmueble_id"),
            rs.getString("numero_recibo"),
            rs.getDate("fecha_emision"),
            rs.getDouble("renta"),
            rs.getDouble("agua"),
            rs.getDouble("luz"),
            rs.getDouble("ipc"),
            rs.getDouble("porteria"),
            rs.getDouble("iva"),
            rs.getDouble("otros_conceptos"),
            rs.getString("descripcion_otros"),
            rs.getBoolean("cobrado")
        );
    }
}
