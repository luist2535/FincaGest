package com.fincas.gui;

import com.fincas.dao.BancoDAO;
import com.fincas.dao.InmuebleDAO;
import com.fincas.dao.ReciboDAO;
import com.fincas.model.Banco;
import com.fincas.model.Recibo;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

public class DashboardPanel extends JPanel {
    private JLabel lblSaldoBanco;
    private JLabel lblInmueblesTotal;
    private JLabel lblInmueblesAlquilados;
    private JLabel lblInmueblesLibres;
    private JLabel lblRecibosCobrados;
    private JLabel lblRecibosPendientes;

    private final BancoDAO bancoDAO = new BancoDAO();
    private final InmuebleDAO inmuebleDAO = new InmuebleDAO();
    private final ReciboDAO reciboDAO = new ReciboDAO();

    public DashboardPanel() {
        setLayout(new BorderLayout());
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Grid de Tarjetas (Cards)
        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 20, 20));
        cardsGrid.setOpaque(false);

        // Tarjeta 1: Saldo total bancos
        cardsGrid.add(createStatCard("🏦 SALDO EN BANCOS", "Cargando...", new Color(14, 165, 233))); // Sky Blue

        // Tarjeta 2: Inmuebles Totales
        cardsGrid.add(createStatCard("🏢 INMUEBLES PROPIOS", "Cargando...", new Color(79, 70, 229))); // Indigo

        // Tarjeta 3: Inmuebles Alquilados
        cardsGrid.add(createStatCard("🔑 ALQUILERES ACTIVOS", "Cargando...", new Color(16, 185, 129))); // Emerald Green

        // Tarjeta 4: Inmuebles Libres
        cardsGrid.add(createStatCard("🔓 ESPACIOS LIBRES", "Cargando...", new Color(245, 158, 11))); // Amber

        // Tarjeta 5: Recibos Cobrados
        cardsGrid.add(createStatCard("✅ RECIBOS COBRADOS", "Cargando...", new Color(34, 197, 94))); // Green

        // Tarjeta 6: Recibos Pendientes
        cardsGrid.add(createStatCard("❌ RECIBOS IMPAGADOS", "Cargando...", new Color(239, 68, 68))); // Red

        add(cardsGrid, BorderLayout.NORTH);

        // Mensaje de bienvenida / Atajos rápidos
        JPanel quickActions = new JPanel(new BorderLayout());
        quickActions.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            " ACCESO RÁPIDO & GUÍA DEL SISTEMA ",
            0, 0, new Font("SansSerif", Font.BOLD, 14), new Color(71, 85, 105)
        ));
        quickActions.setPreferredSize(new Dimension(800, 280));
        
        JTextArea txtGuia = new JTextArea();
        txtGuia.setEditable(false);
        txtGuia.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtGuia.setBackground(null);
        txtGuia.setText(
            "Bienvenido al Panel de Gestión de Fincas Inmobiliarias.\n\n" +
            "Sugerencias de Operaciones Comunes:\n" +
            "1. Gestión de Inmuebles: Registra tus edificios y luego añade pisos o locales dentro de ellos.\n" +
            "2. Inquilinos y Garantías: Da de alta inquilinos. Recuerda que para poder alquilar deben tener garantías válidas (Nómina, Aval o Contrato).\n" +
            "3. Contratos de Alquiler: Asigna un inquilino a un piso o local libre para iniciar la facturación mensual.\n" +
            "4. Facturación mensual: En la pestaña Recibos, genera recibos copiando el mes anterior con un solo clic. Modifica lecturas de luz y agua si es necesario.\n" +
            "5. Contabilidad: Cobra recibos pendientes para aumentar el saldo bancario automáticamente o registra gastos generales (mantenimiento, portería)."
        );
        txtGuia.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        quickActions.add(new JScrollPane(txtGuia), BorderLayout.CENTER);

        add(quickActions, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, String initialVal, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(248, 250, 252)); // Slate 50
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Línea decorativa del color de acento a la izquierda de la tarjeta
        JPanel leftBorder = new JPanel();
        leftBorder.setPreferredSize(new Dimension(5, card.getHeight()));
        leftBorder.setBackground(accentColor);
        card.add(leftBorder, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitle.setForeground(new Color(100, 116, 139)); // Slate 500
        
        JLabel lblVal = new JLabel(initialVal);
        lblVal.setFont(new Font("Outfit", Font.BOLD, 28));
        lblVal.setForeground(new Color(30, 41, 59)); // Slate 800

        infoPanel.add(lblTitle);
        infoPanel.add(lblVal);

        // Guardamos las referencias de los Labels para poder actualizarlos en refreshData
        if (title.contains("BANCOS")) lblSaldoBanco = lblVal;
        else if (title.contains("PROPIOS")) lblInmueblesTotal = lblVal;
        else if (title.contains("ALQUILERES")) lblInmueblesAlquilados = lblVal;
        else if (title.contains("LIBRES")) lblInmueblesLibres = lblVal;
        else if (title.contains("COBRADOS")) lblRecibosCobrados = lblVal;
        else if (title.contains("IMPAGADOS")) lblRecibosPendientes = lblVal;

        card.add(infoPanel, BorderLayout.CENTER);
        return card;
    }

    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            double totalSaldo = 0;
            int totalInmuebles = 0;
            int alquilados = 0;
            int libres = 0;
            int cobradosMes = 0;
            int pendientesMes = 0;

            @Override
            protected Void doInBackground() throws Exception {
                // 1. Bancos
                List<Banco> bancos = bancoDAO.getAll();
                for (Banco b : bancos) {
                    totalSaldo += b.getSaldo();
                }

                // 2. Inmuebles
                totalInmuebles = inmuebleDAO.getAll().size();
                alquilados = inmuebleDAO.getDisponiblesParaAlquiler().size(); // Libres de alquiler
                libres = alquilados;
                
                // Corrección: alquileres activos
                String sqlActivos = "SELECT COUNT(*) FROM alquileres WHERE activo = 1";
                try (var conn = com.fincas.db.DatabaseConnection.getConnection();
                     var stmt = conn.createStatement();
                     var rs = stmt.executeQuery(sqlActivos)) {
                    if (rs.next()) {
                        alquilados = rs.getInt(1);
                    }
                }
                libres = totalInmuebles - alquilados;

                // 3. Recibos del mes actual
                Calendar cal = Calendar.getInstance();
                int mes = cal.get(Calendar.MONTH) + 1;
                int anio = cal.get(Calendar.YEAR);
                
                List<Recibo> recibos = reciboDAO.getAll();
                for (Recibo r : recibos) {
                    Calendar rCal = Calendar.getInstance();
                    rCal.setTime(r.getFechaEmision());
                    if (rCal.get(Calendar.MONTH) + 1 == mes && rCal.get(Calendar.YEAR) == anio) {
                        if (r.isCobrado()) {
                            cobradosMes++;
                        } else {
                            pendientesMes++;
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Levanta excepciones si las hay
                    lblSaldoBanco.setText(String.format("%.2f", totalSaldo) + " €");
                    lblInmueblesTotal.setText(String.valueOf(totalInmuebles));
                    lblInmueblesAlquilados.setText(String.valueOf(alquilados));
                    lblInmueblesLibres.setText(String.valueOf(libres));
                    lblRecibosCobrados.setText(String.valueOf(cobradosMes));
                    lblRecibosPendientes.setText(String.valueOf(pendientesMes));
                } catch (Exception e) {
                    System.err.println("Error al cargar estadísticas: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
