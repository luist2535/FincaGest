package com.fincas.gui;

import com.fincas.dao.InmuebleDAO;
import com.fincas.dao.InquilinoDAO;
import com.fincas.dao.MovimientoBancarioDAO;
import com.fincas.dao.ReciboDAO;
import com.fincas.model.Inmueble;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class InformesPanel extends JPanel {
    private JComboBox<String> cbReportType;
    private JPanel filterPanel;
    private JTextField txtStart, txtEnd;
    private JComboBox<String> cbPaymentStatus;
    private JButton btnGenerate, btnPrintReport;
    
    // Contenedores de resultados
    private CardLayout resultCardLayout;
    private JPanel resultCardPanel;
    private JTable tblResults;
    private DefaultTableModel modelResults;
    private JTextArea txtTextResults;

    private final InquilinoDAO inquilinoDAO = new InquilinoDAO();
    private final InmuebleDAO inmuebleDAO = new InmuebleDAO();
    private final ReciboDAO reciboDAO = new ReciboDAO();
    private final MovimientoBancarioDAO movDAO = new MovimientoBancarioDAO();

    public InformesPanel() {
        setLayout(new BorderLayout());
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // --- Panel Superior de Filtros e Selección ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 15, 5));

        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        selectionPanel.add(new JLabel("Seleccione Informe:"));
        
        String[] reports = {
            "Listado de Inquilinos Ordenado por Fechas de Alquiler",
            "Estado de Pagos de Inquilinos en un Intervalo",
            "Pisos y Locales Agrupados por Edificio",
            "Recibos Pendientes de Cobro en un Rango de Fechas",
            "Informe Declaración de la Renta (Resumen Contable)"
        };
        cbReportType = new JComboBox<>(reports);
        cbReportType.addActionListener(e -> updateFilterInputs());
        selectionPanel.add(cbReportType);
        
        topPanel.add(selectionPanel, BorderLayout.NORTH);

        // Subpanel de filtros dinámicos
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        
        filterPanel.add(new JLabel("Fecha Inicio (YYYY-MM-DD):"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        txtStart = new JTextField(10);
        // Poner fecha de hace 1 mes
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, -1);
        txtStart.setText(sdf.format(cal.getTime()));
        filterPanel.add(txtStart);

        filterPanel.add(new JLabel("Fecha Fin (YYYY-MM-DD):"));
        txtEnd = new JTextField(10);
        txtEnd.setText(sdf.format(new java.util.Date()));
        filterPanel.add(txtEnd);

        filterPanel.add(new JLabel("Estado:"));
        cbPaymentStatus = new JComboBox<>(new String[]{"Cobrados (Pagados)", "Pendientes (Impagos)"});
        filterPanel.add(cbPaymentStatus);

        btnGenerate = new JButton("⚡ Generar Informe");
        styleButton(btnGenerate, new Color(79, 70, 229), Color.WHITE);
        filterPanel.add(btnGenerate);

        btnPrintReport = new JButton("🖨️ Imprimir Resultados");
        styleButton(btnPrintReport, new Color(14, 165, 233), Color.WHITE);
        filterPanel.add(btnPrintReport);

        topPanel.add(filterPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // --- Panel Central de Resultados (CardLayout) ---
        resultCardLayout = new CardLayout();
        resultCardPanel = new JPanel(resultCardLayout);

        // Vista de Tabla
        modelResults = new DefaultTableModel();
        tblResults = new JTable(modelResults);
        tblResults.setRowHeight(25);
        tblResults.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tblResults.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        resultCardPanel.add(new JScrollPane(tblResults), "TABLE");

        // Vista de Texto (para Declaración de la Renta)
        txtTextResults = new JTextArea();
        txtTextResults.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtTextResults.setEditable(false);
        txtTextResults.setMargin(new Insets(15, 15, 15, 15));
        resultCardPanel.add(new JScrollPane(txtTextResults), "TEXT");

        add(resultCardPanel, BorderLayout.CENTER);

        // Eventos
        btnGenerate.addActionListener(e -> generateReport());
        btnPrintReport.addActionListener(e -> printCurrentReport());

        // Inicializar interfaz de filtros
        updateFilterInputs();
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    public void refreshData() {
        // No necesita precarga, el usuario elige cuándo generar
    }

    private void updateFilterInputs() {
        int index = cbReportType.getSelectedIndex();
        // 0: Inquilinos por Fecha -> No ocupa filtros
        // 1: Pagos de Inquilinos en Intervalo -> Ocupa fechas y estado
        // 2: Pisos/Locales por Edificio -> No ocupa filtros
        // 3: Recibos pendientes en Rango -> Ocupa rango de fechas (no estado, es implícito)
        // 4: Declaración de la Renta -> Ocupa rango de fechas
        
        txtStart.setVisible(index == 1 || index == 3 || index == 4);
        txtEnd.setVisible(index == 1 || index == 3 || index == 4);
        cbPaymentStatus.setVisible(index == 1);
        
        // Ocultar etiquetas correspondientes
        for (Component c : filterPanel.getComponents()) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                if (l.getText().contains("Inicio") || l.getText().contains("Fin")) {
                    l.setVisible(index == 1 || index == 3 || index == 4);
                } else if (l.getText().contains("Estado")) {
                    l.setVisible(index == 1);
                }
            }
        }
        
        revalidate();
        repaint();
    }

    private void generateReport() {
        int index = cbReportType.getSelectedIndex();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date start = null;
        Date end = null;

        if (txtStart.isVisible()) {
            try {
                start = new Date(sdf.parse(txtStart.getText().trim()).getTime());
                end = new Date(sdf.parse(txtEnd.getText().trim()).getTime());
            } catch (Exception ex) {
                showWarning("Por favor introduzca fechas válidas en formato AAAA-MM-DD.");
                return;
            }
        }

        try {
            switch (index) {
                case 0:
                    generateTenantsSortedReport();
                    break;
                case 1:
                    generateTenantsPaymentReport(start, end, cbPaymentStatus.getSelectedIndex() == 0);
                    break;
                case 2:
                    generatePropertiesGroupedReport();
                    break;
                case 3:
                    generatePendingReceiptsReport(start, end);
                    break;
                case 4:
                    generateIncomeTaxReport(start, end);
                    break;
            }
        } catch (SQLException ex) {
            showError("Error al generar informe: " + ex.getMessage());
        }
    }

    private void generateTenantsSortedReport() throws SQLException {
        resultCardLayout.show(resultCardPanel, "TABLE");
        
        String[] cols = {"DNI", "Inquilino", "Edad", "Sexo", "Garantía", "F. Inicio Alquiler", "Inmueble Alquilado"};
        modelResults.setDataVector(null, cols);

        List<Object[]> rows = inquilinoDAO.getTenantsSortedByDate();
        for (Object[] row : rows) {
            modelResults.addRow(new Object[]{
                row[0], row[1], row[2], row[3], row[4], 
                row[5] != null ? row[5].toString() : "Sin Contrato",
                row[6]
            });
        }
    }

    private void generateTenantsPaymentReport(Date start, Date end, boolean paid) throws SQLException {
        resultCardLayout.show(resultCardPanel, "TABLE");
        String[] cols = {"DNI Inquilino", "Inquilino", "Fecha Emisión Recibo", "Código Recibo", "Inmueble", "Estado de Cobro"};
        modelResults.setDataVector(null, cols);

        List<Object[]> rows = inquilinoDAO.getTenantsPaymentStatus(start, end, paid);
        for (Object[] row : rows) {
            modelResults.addRow(row);
        }
    }

    private void generatePropertiesGroupedReport() throws SQLException {
        resultCardLayout.show(resultCardPanel, "TABLE");
        String[] cols = {"Inmueble Principal / Edificio", "Código Finca", "Tipo", "Dirección", "Planta", "Letra", "Código Recibo"};
        modelResults.setDataVector(null, cols);

        List<Inmueble> all = inmuebleDAO.getAll();
        for (Inmueble i : all) {
            if ("EDIFICIO".equals(i.getTipo())) {
                modelResults.addRow(new Object[]{
                    "🏢 EDIFICIO ID: " + i.getId(),
                    i.getCodigoPostal(),
                    i.getTipo(),
                    i.getDireccion() + ", No. " + i.getNumero(),
                    "-",
                    "-",
                    "-"
                });

                // Cargar dependencias
                List<Inmueble> dependencias = inmuebleDAO.getPisosYLocalesDeEdificio(i.getId());
                for (Inmueble dep : dependencias) {
                    modelResults.addRow(new Object[]{
                        "     └─ ID " + dep.getId(),
                        dep.getCodigoPostal(),
                        "   " + dep.getTipo(),
                        "   " + dep.getDireccion() + ", No. " + dep.getNumero(),
                        dep.getPlanta(),
                        dep.getLetra(),
                        dep.getCodigoRecibo()
                    });
                }
            } else if (i.getParentEdificioId() == null) {
                // Independiente
                modelResults.addRow(new Object[]{
                    "📦 INDEPENDIENTE ID: " + i.getId(),
                    i.getCodigoPostal(),
                    i.getTipo(),
                    i.getDireccion() + ", No. " + i.getNumero(),
                    i.getPlanta() != null ? i.getPlanta() : "-",
                    i.getLetra() != null ? i.getLetra() : "-",
                    i.getCodigoRecibo() != null ? i.getCodigoRecibo() : "-"
                });
            }
        }
    }

    private void generatePendingReceiptsReport(Date start, Date end) throws SQLException {
        resultCardLayout.show(resultCardPanel, "TABLE");
        String[] cols = {"ID Recibo", "Código Recibo", "Fecha Emisión", "Inmueble Asociado", "Total Facturado"};
        modelResults.setDataVector(null, cols);

        List<Object[]> rows = reciboDAO.getRecibosPendientesIntervalo(start, end);
        for (Object[] row : rows) {
            modelResults.addRow(new Object[]{
                row[0], row[1], row[2], row[3], 
                String.format("%.2f €", row[4])
            });
        }
    }

    // Exigencia del enunciado: Reporte contable para la declaración de la renta
    private void generateIncomeTaxReport(Date start, Date end) throws SQLException {
        resultCardLayout.show(resultCardPanel, "TEXT");

        Map<String, Object> data = movDAO.getResumenDeclaracionRenta(start, end);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================================\n");
        sb.append("         INFORME ECONÓMICO - DECLARACIÓN DE LA RENTA (I.R.P.F.)          \n");
        sb.append("=========================================================================\n");
        sb.append(" Rango de Fechas Analizado: ").append(sdf.format(start)).append(" al ").append(sdf.format(end)).append("\n");
        sb.append(" Generado automáticamente por: FincaGest S.A. Administración\n");
        sb.append("=========================================================================\n\n");

        sb.append("1. INGRESOS COMPUTABLES (Rentas percibidas de pisos y locales)\n");
        sb.append("-------------------------------------------------------------------------\n");
        List<Object[]> ingresos = (List<Object[]>) data.get("ingresos");
        if (ingresos.isEmpty()) {
            sb.append("   No se registran ingresos en este intervalo de tiempo.\n");
        } else {
            for (Object[] ing : ingresos) {
                sb.append(String.format("   Categoría: %-25s Total: %15.2f €\n", ing[0], ing[1]));
            }
        }
        sb.append(String.format("   >> TOTAL INGRESOS:                                      %15.2f €\n\n\n", data.get("total_ingresos")));

        sb.append("2. GASTOS DEDUCIBLES (Costes de reparación, limpieza y portería)\n");
        sb.append("-------------------------------------------------------------------------\n");
        List<Object[]> gastos = (List<Object[]>) data.get("gastos");
        if (gastos.isEmpty()) {
            sb.append("   No se registran gastos deducibles en este intervalo de tiempo.\n");
        } else {
            for (Object[] gas : gastos) {
                sb.append(String.format("   Categoría: %-25s Total: %15.2f €\n", gas[0], gas[1]));
            }
        }
        sb.append(String.format("   >> TOTAL GASTOS DEDUCIBLES:                             %15.2f €\n\n\n", data.get("total_gastos")));

        sb.append("3. RENDIMIENTO NETO DE LA ACTIVIDAD (Base Imponible)\n");
        sb.append("-------------------------------------------------------------------------\n");
        double rendimiento = (double) data.get("rendimiento_neto");
        sb.append(String.format("   Ingresos Computables:   %15.2f €\n", data.get("total_ingresos")));
        sb.append(String.format("   (-) Gastos Deducibles:  %15.2f €\n", data.get("total_gastos")));
        sb.append("                           ----------------------\n");
        sb.append(String.format("   RENDIMIENTO NETO:       %15.2f €\n", rendimiento));
        sb.append("=========================================================================\n");
        sb.append(" Nota: Este informe es un borrador administrativo para facilitar la declaración\n");
        sb.append(" fiscal anual de la propiedad del capital inmobiliario.\n");
        sb.append("=========================================================================\n");

        txtTextResults.setText(sb.toString());
    }

    private void printCurrentReport() {
        try {
            int index = cbReportType.getSelectedIndex();
            if (index == 4) {
                // Impresión de texto simple
                txtTextResults.print();
            } else {
                // Impresión de tabla estructurada
                tblResults.print(JTable.PrintMode.FIT_WIDTH, 
                    new MessageFormat("Informe: " + cbReportType.getSelectedItem()), 
                    new MessageFormat("Página {0}"));
            }
        } catch (Exception ex) {
            showError("Error al imprimir el reporte: " + ex.getMessage());
        }
    }

    private void showWarning(String m) { JOptionPane.showMessageDialog(this, m, "Advertencia", JOptionPane.WARNING_MESSAGE); }
    private void showError(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
}
