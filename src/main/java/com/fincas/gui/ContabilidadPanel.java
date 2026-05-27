package com.fincas.gui;

import com.fincas.dao.BancoDAO;
import com.fincas.dao.InmuebleDAO;
import com.fincas.dao.MovimientoBancarioDAO;
import com.fincas.model.Banco;
import com.fincas.model.Inmueble;
import com.fincas.model.MovimientoBancario;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ContabilidadPanel extends JPanel {
    private JTable tblBancos;
    private DefaultTableModel modelBancos;
    private JTable tblMovs;
    private DefaultTableModel modelMovs;

    private JButton btnNewBank, btnNewExpense, btnNewIncome, btnDeleteMov;

    private final BancoDAO bancoDAO = new BancoDAO();
    private final InmuebleDAO inmuebleDAO = new InmuebleDAO();
    private final MovimientoBancarioDAO movDAO = new MovimientoBancarioDAO();

    public ContabilidadPanel() {
        setLayout(new BorderLayout());
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Dividido en panel superior (bancos) e inferior (movimientos)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);

        // --- Panel de Bancos ---
        JPanel pnlBancos = new JPanel(new BorderLayout());
        pnlBancos.setBorder(BorderFactory.createTitledBorder("🏦 Cuentas Bancarias Registradas"));

        JPanel bankTools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnNewBank = new JButton("➕ Nueva Cuenta Bancaria");
        styleButton(btnNewBank, new Color(79, 70, 229), Color.WHITE);
        bankTools.add(btnNewBank);
        pnlBancos.add(bankTools, BorderLayout.NORTH);

        modelBancos = new DefaultTableModel(new String[]{"ID", "Banco", "Número Cuenta", "Saldo Disponible"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBancos = new JTable(modelBancos);
        tblBancos.setRowHeight(25);
        pnlBancos.add(new JScrollPane(tblBancos), BorderLayout.CENTER);

        splitPane.setTopComponent(pnlBancos);

        // --- Panel de Movimientos ---
        JPanel pnlMovs = new JPanel(new BorderLayout());
        pnlMovs.setBorder(BorderFactory.createTitledBorder("💰 Registro de Ingresos y Gastos"));

        JPanel movTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnNewExpense = new JButton("📉 Registrar Gasto (Edificio/Inm)");
        btnNewIncome = new JButton("📈 Registrar Ingreso (Piso/Local)");
        btnDeleteMov = new JButton("❌ Eliminar Movimiento");

        styleButton(btnNewExpense, new Color(239, 68, 68), Color.WHITE); // Red
        styleButton(btnNewIncome, new Color(16, 185, 129), Color.WHITE); // Green
        styleButton(btnDeleteMov, new Color(120, 113, 108), Color.WHITE); // Slate

        movTools.add(btnNewExpense);
        movTools.add(btnNewIncome);
        movTools.add(btnDeleteMov);
        pnlMovs.add(movTools, BorderLayout.NORTH);

        modelMovs = new DefaultTableModel(new String[]{"ID", "Cuenta Bancaria", "Tipo", "Fecha", "Monto", "Categoría", "Inmueble / Piso / Local Asociado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblMovs = new JTable(modelMovs);
        tblMovs.setRowHeight(25);
        pnlMovs.add(new JScrollPane(tblMovs), BorderLayout.CENTER);

        splitPane.setBottomComponent(pnlMovs);

        add(splitPane, BorderLayout.CENTER);

        // Eventos
        btnNewBank.addActionListener(e -> openNewBankDialog());
        btnNewExpense.addActionListener(e -> openMovementDialog("GASTO"));
        btnNewIncome.addActionListener(e -> openMovementDialog("INGRESO"));
        btnDeleteMov.addActionListener(e -> {
            int selectedRow = tblMovs.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) modelMovs.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "¿Estás seguro de eliminar este movimiento bancario?\nSe revertirá el monto de la cuenta bancaria.",
                    "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        if (movDAO.delete(id)) {
                            showInfo("Movimiento eliminado y saldo bancario actualizado.");
                            refreshData();
                        }
                    } catch (SQLException ex) {
                        showError("Error al eliminar movimiento: " + ex.getMessage());
                    }
                }
            } else {
                showWarning("Selecciona un movimiento para eliminar.");
            }
        });
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
        // Cargar Bancos
        modelBancos.setRowCount(0);
        try {
            List<Banco> bancos = bancoDAO.getAll();
            for (Banco b : bancos) {
                modelBancos.addRow(new Object[]{
                    b.getId(),
                    b.getNombreBanco(),
                    b.getNumeroCuenta(),
                    String.format("%.2f €", b.getSaldo())
                });
            }
        } catch (SQLException ex) {
            showError("Error al cargar bancos: " + ex.getMessage());
        }

        // Cargar Movimientos
        modelMovs.setRowCount(0);
        try {
            List<Object[]> movs = movDAO.getMovimientosDetallados();
            for (Object[] m : movs) {
                modelMovs.addRow(new Object[]{
                    m[0], // ID
                    m[1], // Banco
                    m[2], // Tipo
                    m[3], // Fecha
                    String.format("%.2f €", m[4]), // Monto
                    m[5], // Categoria
                    m[6]  // Asociacion
                });
            }
        } catch (SQLException ex) {
            showError("Error al cargar movimientos: " + ex.getMessage());
        }
    }

    private void openNewBankDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nueva Cuenta Bancaria", true);
        dlg.setSize(380, 220);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Nombre Banco:"), gbc);
        JTextField txtBanco = new JTextField(15);
        gbc.gridx = 1;
        dlg.add(txtBanco, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dlg.add(new JLabel("Número Cuenta:"), gbc);
        JTextField txtCuenta = new JTextField(18);
        gbc.gridx = 1;
        dlg.add(txtCuenta, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dlg.add(new JLabel("Saldo Inicial (€):"), gbc);
        JSpinner txtSaldo = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 100.0));
        gbc.gridx = 1;
        dlg.add(txtSaldo, gbc);

        JPanel btnPanel = new JPanel();
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            String bName = txtBanco.getText().trim();
            String accNum = txtCuenta.getText().trim();
            double saldo = (double) txtSaldo.getValue();

            if (bName.isEmpty() || accNum.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Todos los campos son obligatorios.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Banco b = new Banco(0, bName, accNum, saldo);
            try {
                if (bancoDAO.insert(b)) {
                    showInfo("Cuenta bancaria registrada con éxito.");
                    dlg.dispose();
                    refreshData();
                }
            } catch (SQLException ex) {
                showError("No se pudo registrar la cuenta bancaria: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    private void openMovementDialog(String tipo) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Registrar " + (tipo.equals("GASTO") ? "Gasto" : "Ingreso"), true);
        dlg.setSize(480, 340);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Banco
        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Cuenta Bancaria:"), gbc);
        DefaultComboBoxModel<Banco> bankModel = new DefaultComboBoxModel<>();
        try {
            for (Banco b : bancoDAO.getAll()) {
                bankModel.addElement(b);
            }
        } catch (SQLException ex) {
            System.err.println("Error al cargar bancos.");
        }
        JComboBox<Banco> cbBancos = new JComboBox<>(bankModel);
        gbc.gridx = 1;
        dlg.add(cbBancos, gbc);

        // Asociación Inmueble (Gasto -> Edificio / Ingreso -> Piso-Local)
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblAsoc = new JLabel(tipo.equals("GASTO") ? "Asociado a Inmueble (Gasto):" : "Asociado a Piso/Local (Ingreso):");
        dlg.add(lblAsoc, gbc);

        DefaultComboBoxModel<InmuebleComboItem> inmModel = new DefaultComboBoxModel<>();
        try {
            for (Inmueble i : inmuebleDAO.getAll()) {
                // Si es gasto, se asocia a cualquier inmueble (incluido edificios)
                // Si es ingreso, se asocia a un piso o local (no a edificios enteros usualmente, o sí)
                if (tipo.equals("GASTO")) {
                    inmModel.addElement(new InmuebleComboItem(i));
                } else if (!"EDIFICIO".equals(i.getTipo())) {
                    inmModel.addElement(new InmuebleComboItem(i));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al cargar inmuebles.");
        }
        JComboBox<InmuebleComboItem> cbInmuebles = new JComboBox<>(inmModel);
        gbc.gridx = 1;
        dlg.add(cbInmuebles, gbc);

        // Categoría
        gbc.gridx = 0; gbc.gridy = 2;
        dlg.add(new JLabel("Categoría del Movimiento:"), gbc);
        
        String[] categorias = tipo.equals("GASTO") ? 
            new String[]{"REPARACION", "LIMPIEZA", "SUELDO", "OTROS_GASTOS"} :
            new String[]{"RECIBO_ALQUILER", "OTROS_INGRESOS"};
            
        JComboBox<String> cbCat = new JComboBox<>(categorias);
        gbc.gridx = 1;
        dlg.add(cbCat, gbc);

        // Importe
        gbc.gridx = 0; gbc.gridy = 3;
        dlg.add(new JLabel("Importe (€):"), gbc);
        JSpinner txtImporte = new JSpinner(new SpinnerNumberModel(10.0, 0.01, 999999.0, 10.0));
        gbc.gridx = 1;
        dlg.add(txtImporte, gbc);

        // Fecha
        gbc.gridx = 0; gbc.gridy = 4;
        dlg.add(new JLabel("Fecha (YYYY-MM-DD):"), gbc);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JTextField txtFecha = new JTextField(sdf.format(new java.util.Date()));
        gbc.gridx = 1;
        dlg.add(txtFecha, gbc);

        // Botones
        JPanel btnPanel = new JPanel();
        JButton btnSave = new JButton("Registrar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            Banco b = (Banco) cbBancos.getSelectedItem();
            InmuebleComboItem inmItem = (InmuebleComboItem) cbInmuebles.getSelectedItem();
            
            if (b == null) {
                JOptionPane.showMessageDialog(dlg, "Debe seleccionar una cuenta bancaria.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (inmItem == null || inmItem.getInmueble() == null) {
                JOptionPane.showMessageDialog(dlg, "Debe asociar este movimiento a un inmueble.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date fMov;
            try {
                fMov = new Date(sdf.parse(txtFecha.getText().trim()).getTime());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Fecha inválida. Utilice AAAA-MM-DD.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double importe = (double) txtImporte.getValue();
            String cat = cbCat.getSelectedItem().toString();

            Integer inmuebleId = null;
            Integer pisoLocalId = null;

            if (tipo.equals("GASTO")) {
                inmuebleId = inmItem.getInmueble().getId();
            } else {
                pisoLocalId = inmItem.getInmueble().getId();
            }

            MovimientoBancario m = new MovimientoBancario(0, b.getId(), tipo, fMov, importe, cat, inmuebleId, pisoLocalId);
            try {
                if (movDAO.insert(m)) {
                    showInfo("Movimiento bancario registrado correctamente.");
                    dlg.dispose();
                    refreshData();
                }
            } catch (SQLException ex) {
                showError("No se pudo registrar el movimiento: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    private void showInfo(String m) { JOptionPane.showMessageDialog(this, m, "Información", JOptionPane.INFORMATION_MESSAGE); }
    private void showWarning(String m) { JOptionPane.showMessageDialog(this, m, "Advertencia", JOptionPane.WARNING_MESSAGE); }
    private void showError(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }

    private static class InmuebleComboItem {
        private final Inmueble inm;
        public InmuebleComboItem(Inmueble i) { this.inm = i; }
        public Inmueble getInmueble() { return inm; }
        @Override
        public String toString() {
            String desc = inm.getTipo() + ": " + inm.getDireccion();
            if (inm.getPlanta() != null && !inm.getPlanta().isEmpty()) desc += " " + inm.getPlanta() + "º";
            if (inm.getLetra() != null && !inm.getLetra().isEmpty()) desc += " " + inm.getLetra();
            return desc;
        }
    }
}
