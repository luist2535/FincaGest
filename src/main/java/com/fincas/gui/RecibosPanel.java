package com.fincas.gui;

import com.fincas.dao.BancoDAO;
import com.fincas.dao.ReciboDAO;
import com.fincas.model.Banco;
import com.fincas.model.Recibo;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class RecibosPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnGenerate, btnEdit, btnInitConcept, btnCollect, btnPrint;
    
    private final ReciboDAO reciboDAO = new ReciboDAO();
    private final BancoDAO bancoDAO = new BancoDAO();

    public RecibosPanel() {
        setLayout(new BorderLayout());
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Barra de Herramientas Superior
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolBar.setOpaque(false);

        btnGenerate = new JButton("🗓️ Generar Recibos del Mes");
        btnEdit = new JButton("✏️ Modificar Recibo");
        btnInitConcept = new JButton("⚙️ Inicializar Concepto");
        btnCollect = new JButton("💰 Registrar Cobro");
        btnPrint = new JButton("🖨️ Formato Impreso");

        styleButton(btnGenerate, new Color(79, 70, 229), Color.WHITE);
        styleButton(btnEdit, new Color(245, 158, 11), Color.WHITE);
        styleButton(btnInitConcept, new Color(71, 85, 105), Color.WHITE);
        styleButton(btnCollect, new Color(16, 185, 129), Color.WHITE);
        styleButton(btnPrint, new Color(14, 165, 233), Color.WHITE);

        toolBar.add(btnGenerate);
        toolBar.add(btnEdit);
        toolBar.add(btnInitConcept);
        toolBar.add(btnCollect);
        toolBar.add(btnPrint);

        add(toolBar, BorderLayout.NORTH);

        // Tabla de Recibos
        String[] columns = {"ID", "Cód. Recibo", "Fecha Emisión", "Inmueble", "Total", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Eventos
        btnGenerate.addActionListener(e -> triggerGeneration());
        
        btnEdit.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                try {
                    Recibo r = reciboDAO.getById(id);
                    openEditDialog(r);
                } catch (SQLException ex) {
                    showError("Error al consultar recibo: " + ex.getMessage());
                }
            } else {
                showWarning("Selecciona un recibo para modificar.");
            }
        });

        btnInitConcept.addActionListener(e -> openInitConceptDialog());

        btnCollect.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                String estado = (String) tableModel.getValueAt(selectedRow, 5);
                if ("Cobrado".equals(estado)) {
                    showWarning("Este recibo ya está registrado como cobrado.");
                    return;
                }
                openCollectDialog(id);
            } else {
                showWarning("Selecciona un recibo pendiente para cobrar.");
            }
        });

        btnPrint.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                try {
                    Recibo r = reciboDAO.getById(id);
                    String desc = (String) tableModel.getValueAt(selectedRow, 3);
                    openPrintDialog(r, desc);
                } catch (SQLException ex) {
                    showError("Error al cargar recibo: " + ex.getMessage());
                }
            } else {
                showWarning("Selecciona un recibo para ver su formato impreso.");
            }
        });
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    public void refreshData() {
        tableModel.setRowCount(0);
        try {
            List<Object[]> list = reciboDAO.getRecibosDetallados();
            for (Object[] row : list) {
                tableModel.addRow(new Object[]{
                    row[0], // ID
                    row[1], // Codigo
                    row[2], // Fecha Emision
                    row[4], // Inmueble desc
                    String.format("%.2f €", row[13]), // Total
                    row[3]  // Estado
                });
            }
        } catch (SQLException ex) {
            showError("Error al cargar recibos: " + ex.getMessage());
        }
    }

    private void triggerGeneration() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String inputDate = JOptionPane.showInputDialog(this, 
            "Introduzca la fecha de emisión para los nuevos recibos (YYYY-MM-DD):",
            sdf.format(new java.util.Date()));
        
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return;
        }

        try {
            Date fecha = new Date(sdf.parse(inputDate.trim()).getTime());
            int generated = reciboDAO.generarRecibosMesAnterior(fecha);
            showInfo("Se han generado " + generated + " recibos correspondientes al mes indicado,\n" +
                     "clonando los conceptos del mes anterior para los alquileres activos.");
            refreshData();
        } catch (Exception ex) {
            showError("Error en la generación: " + ex.getMessage());
        }
    }

    private void openEditDialog(Recibo r) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Modificar Recibo", true);
        dlg.setSize(450, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Mostrar Código y Fecha
        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Código Recibo:"), gbc);
        JLabel lblCod = new JLabel(r.getNumeroRecibo());
        lblCod.setFont(new Font("SansSerif", Font.BOLD, 12));
        gbc.gridx = 1;
        dlg.add(lblCod, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dlg.add(new JLabel("Fecha Emisión (YYYY-MM-DD):"), gbc);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JTextField txtFecha = new JTextField(sdf.format(r.getFechaEmision()));
        gbc.gridx = 1;
        dlg.add(txtFecha, gbc);

        // Conceptos
        gbc.gridx = 0; gbc.gridy = 2;
        dlg.add(new JLabel("Renta Base (€):"), gbc);
        JSpinner txtRenta = new JSpinner(new SpinnerNumberModel(r.getRenta(), 0.0, 99999.0, 10.0));
        gbc.gridx = 1;
        dlg.add(txtRenta, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        dlg.add(new JLabel("Agua (€):"), gbc);
        JSpinner txtAgua = new JSpinner(new SpinnerNumberModel(r.getAgua(), 0.0, 9999.0, 1.0));
        gbc.gridx = 1;
        dlg.add(txtAgua, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        dlg.add(new JLabel("Luz (€):"), gbc);
        JSpinner txtLuz = new JSpinner(new SpinnerNumberModel(r.getLuz(), 0.0, 9999.0, 1.0));
        gbc.gridx = 1;
        dlg.add(txtLuz, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        dlg.add(new JLabel("Actualización IPC (€):"), gbc);
        JSpinner txtIPC = new JSpinner(new SpinnerNumberModel(r.getIpc(), 0.0, 9999.0, 1.0));
        gbc.gridx = 1;
        dlg.add(txtIPC, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        dlg.add(new JLabel("Portería (€):"), gbc);
        JSpinner txtPorteria = new JSpinner(new SpinnerNumberModel(r.getPorteria(), 0.0, 9999.0, 1.0));
        gbc.gridx = 1;
        dlg.add(txtPorteria, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        dlg.add(new JLabel("IVA (€):"), gbc);
        JSpinner txtIVA = new JSpinner(new SpinnerNumberModel(r.getIva(), 0.0, 9999.0, 1.0));
        gbc.gridx = 1;
        dlg.add(txtIVA, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        dlg.add(new JLabel("Otros Conceptos (€):"), gbc);
        JSpinner txtOtros = new JSpinner(new SpinnerNumberModel(r.getOtrosConceptos(), 0.0, 9999.0, 1.0));
        gbc.gridx = 1;
        dlg.add(txtOtros, gbc);

        gbc.gridx = 0; gbc.gridy = 9;
        dlg.add(new JLabel("Detalle Otros:"), gbc);
        JTextField txtDescOtros = new JTextField(r.getDescripcionOtros() != null ? r.getDescripcionOtros() : "");
        gbc.gridx = 1;
        dlg.add(txtDescOtros, gbc);

        // Cobrado
        gbc.gridx = 0; gbc.gridy = 10;
        dlg.add(new JLabel("Estado:"), gbc);
        JCheckBox chkCobrado = new JCheckBox("Cobrado / Pagado");
        chkCobrado.setSelected(r.isCobrado());
        gbc.gridx = 1;
        dlg.add(chkCobrado, gbc);

        // Botones
        JPanel btnPanel = new JPanel();
        JButton btnSave = new JButton("Guardar Cambios");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());
        
        btnSave.addActionListener(e -> {
            Date fEmision;
            try {
                fEmision = new Date(sdf.parse(txtFecha.getText().trim()).getTime());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Fecha inválida (AAAA-MM-DD).", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            r.setFechaEmision(fEmision);
            r.setRenta((double) txtRenta.getValue());
            r.setAgua((double) txtAgua.getValue());
            r.setLuz((double) txtLuz.getValue());
            r.setIpc((double) txtIPC.getValue());
            r.setPorteria((double) txtPorteria.getValue());
            r.setIva((double) txtIVA.getValue());
            r.setOtrosConceptos((double) txtOtros.getValue());
            r.setDescripcionOtros(txtDescOtros.getText().trim());
            r.setCobrado(chkCobrado.isSelected());

            try {
                if (reciboDAO.update(r)) {
                    showInfo("Recibo modificado con éxito.");
                    dlg.dispose();
                    refreshData();
                }
            } catch (SQLException ex) {
                showError("No se pudo actualizar el recibo: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    private void openInitConceptDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Inicializar Concepto", true);
        dlg.setSize(380, 240);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Concepto
        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Concepto a Inicializar:"), gbc);
        
        // Mapeo amigable
        String[] conceptosUser = {"Renta", "Agua", "Luz", "IPC", "Portería", "IVA", "Otros Conceptos"};
        String[] conceptosBD = {"renta", "agua", "luz", "ipc", "porteria", "iva", "otros_conceptos"};
        
        JComboBox<String> cbConcepto = new JComboBox<>(conceptosUser);
        gbc.gridx = 1;
        dlg.add(cbConcepto, gbc);

        // Cantidad
        gbc.gridx = 0; gbc.gridy = 1;
        dlg.add(new JLabel("Cantidad (€):"), gbc);
        JSpinner txtMonto = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 99999.0, 5.0));
        gbc.gridx = 1;
        dlg.add(txtMonto, gbc);

        // Mes / Año
        gbc.gridx = 0; gbc.gridy = 2;
        dlg.add(new JLabel("Mes (1-12) / Año:"), gbc);
        JPanel pnlFecha = new JPanel(new GridLayout(1, 2, 5, 0));
        Calendar cal = Calendar.getInstance();
        JSpinner txtMes = new JSpinner(new SpinnerNumberModel(cal.get(Calendar.MONTH) + 1, 1, 12, 1));
        JSpinner txtAnio = new JSpinner(new SpinnerNumberModel(cal.get(Calendar.YEAR), 2000, 2100, 1));
        pnlFecha.add(txtMes);
        pnlFecha.add(txtAnio);
        gbc.gridx = 1;
        dlg.add(pnlFecha, gbc);

        // Botones
        JPanel btnPanel = new JPanel();
        JButton btnInit = new JButton("Inicializar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnInit);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());
        
        btnInit.addActionListener(e -> {
            String conceptoBD = conceptosBD[cbConcepto.getSelectedIndex()];
            double cant = (double) txtMonto.getValue();
            int mes = (int) txtMes.getValue();
            int anio = (int) txtAnio.getValue();

            int confirm = JOptionPane.showConfirmDialog(dlg, 
                "¿Estás seguro de establecer el concepto '" + cbConcepto.getSelectedItem() + "' a " + cant + " €\n" +
                "para TODOS los recibos del mes " + mes + "/" + anio + "?",
                "Confirmar Inicialización", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    int modificados = reciboDAO.inicializarConcepto(conceptoBD, cant, mes, anio);
                    showInfo("Operación completada. Se modificaron " + modificados + " recibos.");
                    dlg.dispose();
                    refreshData();
                } catch (SQLException ex) {
                    showError("No se pudo completar la operación: " + ex.getMessage());
                }
            }
        });

        dlg.setVisible(true);
    }

    private void openCollectDialog(int reciboId) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Registrar Cobro de Recibo", true);
        dlg.setSize(400, 180);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Seleccione Cuenta de Depósito:"), gbc);
        
        DefaultComboBoxModel<Banco> comboBancoModel = new DefaultComboBoxModel<>();
        try {
            List<Banco> bancos = bancoDAO.getAll();
            for (Banco b : bancos) {
                comboBancoModel.addElement(b);
            }
        } catch (SQLException ex) {
            System.err.println("Error al cargar bancos.");
        }
        
        JComboBox<Banco> cbBancos = new JComboBox<>(comboBancoModel);
        gbc.gridx = 1;
        dlg.add(cbBancos, gbc);

        JPanel btnPanel = new JPanel();
        JButton btnPay = new JButton("Confirmar Cobro");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnPay);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnPay.addActionListener(e -> {
            Banco selectedBanco = (Banco) cbBancos.getSelectedItem();
            if (selectedBanco == null) {
                JOptionPane.showMessageDialog(dlg, "Debe seleccionar un banco para registrar el ingreso.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (reciboDAO.cobrarRecibo(reciboId, selectedBanco.getId())) {
                    showInfo("Recibo cobrado correctamente.\nSe ha registrado un ingreso en la cuenta y actualizado su saldo.");
                    dlg.dispose();
                    refreshData();
                }
            } catch (SQLException ex) {
                showError("Error al registrar el cobro: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    // Exigencia del enunciado: Formato impreso omitiendo conceptos con importe igual a cero
    private void openPrintDialog(Recibo r, String inmuebleDesc) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Vista de Impresión de Recibo", true);
        dlg.setSize(480, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JTextArea txtPrint = new JTextArea();
        txtPrint.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtPrint.setEditable(false);
        txtPrint.setMargin(new Insets(20, 20, 20, 20));

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("                  RECIBO DE ALQUILER                \n");
        sb.append("====================================================\n\n");
        sb.append(" CÓDIGO DE FACTURACIÓN: ").append(r.getNumeroRecibo()).append("\n");
        sb.append(" FECHA DE EMISIÓN:     ").append(new SimpleDateFormat("dd/MM/yyyy").format(r.getFechaEmision())).append("\n");
        sb.append(" INMUEBLE ASOCIADO:    ").append(inmuebleDesc).append("\n");
        sb.append(" ESTADO DEL RECIBO:    ").append(r.isCobrado() ? "COBRADO" : "PENDIENTE DE PAGO").append("\n");
        sb.append("----------------------------------------------------\n");
        sb.append(" CONCEPTOS FACTURADOS:\n\n");

        if (r.getRenta() > 0)        sb.append(String.format("  - Renta Base:                 %10.2f €\n", r.getRenta()));
        if (r.getAgua() > 0)        sb.append(String.format("  - Lectura de Agua:            %10.2f €\n", r.getAgua()));
        if (r.getLuz() > 0)         sb.append(String.format("  - Lectura de Luz:             %10.2f €\n", r.getLuz()));
        if (r.getIpc() > 0)         sb.append(String.format("  - Actualización IPC Anual:     %10.2f €\n", r.getIpc()));
        if (r.getPorteria() > 0)    sb.append(String.format("  - Servicio de Portería:       %10.2f €\n", r.getPorteria()));
        if (r.getIva() > 0)         sb.append(String.format("  - Impuesto IVA:               %10.2f €\n", r.getIva()));
        
        if (r.getOtrosConceptos() > 0) {
            String desc = r.getDescripcionOtros().isEmpty() ? "Otros conceptos" : r.getDescripcionOtros();
            if (desc.length() > 25) desc = desc.substring(0, 22) + "...";
            sb.append(String.format("  - %-28s %10.2f €\n", desc, r.getOtrosConceptos()));
        }

        sb.append("----------------------------------------------------\n");
        sb.append(String.format(" TOTAL NETO RECIBIDO:           %10.2f €\n", r.getTotal()));
        sb.append("====================================================\n\n");
        sb.append("        Gracias por su confianza y puntualidad.      \n");
        sb.append("             FincaGest S.A. Administración           \n");
        sb.append("====================================================\n");

        txtPrint.setText(sb.toString());
        dlg.add(new JScrollPane(txtPrint), BorderLayout.CENTER);

        JButton btnPrintAction = new JButton("🖨️ Enviar a Impresora");
        btnPrintAction.addActionListener(e -> {
            try {
                txtPrint.print();
            } catch (Exception ex) {
                showError("Error al imprimir: " + ex.getMessage());
            }
        });
        
        JPanel pnlSouth = new JPanel();
        pnlSouth.add(btnPrintAction);
        dlg.add(pnlSouth, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void showInfo(String m) { JOptionPane.showMessageDialog(this, m, "Información", JOptionPane.INFORMATION_MESSAGE); }
    private void showWarning(String m) { JOptionPane.showMessageDialog(this, m, "Advertencia", JOptionPane.WARNING_MESSAGE); }
    private void showError(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
}
