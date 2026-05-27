package com.fincas.gui;

import com.fincas.dao.InmuebleDAO;
import com.fincas.model.Inmueble;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class InmueblesPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> cbFilter;
    private JButton btnAdd, btnEdit, btnDelete, btnViewSub;
    private final InmuebleDAO dao = new InmuebleDAO();

    public InmueblesPanel() {
        setLayout(new BorderLayout());
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Barra de Herramientas Superior
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        toolBar.setOpaque(false);

        JLabel lblFilter = new JLabel("Filtrar por Tipo:");
        lblFilter.setFont(new Font("SansSerif", Font.BOLD, 12));
        toolBar.add(lblFilter);

        cbFilter = new JComboBox<>(new String[]{"TODOS", "EDIFICIO", "PISO", "LOCAL"});
        cbFilter.addActionListener(e -> applyFilter());
        toolBar.add(cbFilter);

        // Espaciador
        toolBar.add(Box.createHorizontalStrut(50));

        btnAdd = new JButton("➕ Nuevo Inmueble");
        btnEdit = new JButton("✏️ Modificar");
        btnDelete = new JButton("❌ Dar de Baja");
        btnViewSub = new JButton("📂 Ver Dependencias");

        // Styling de botones
        styleButton(btnAdd, new Color(79, 70, 229), Color.WHITE);
        styleButton(btnEdit, new Color(245, 158, 11), Color.WHITE);
        styleButton(btnDelete, new Color(239, 68, 68), Color.WHITE);
        styleButton(btnViewSub, new Color(14, 165, 233), Color.WHITE);

        toolBar.add(btnAdd);
        toolBar.add(btnEdit);
        toolBar.add(btnDelete);
        toolBar.add(btnViewSub);

        add(toolBar, BorderLayout.NORTH);

        // Tabla de Inmuebles
        String[] columns = {"ID", "Tipo", "Dirección", "Número", "Código Postal", "Planta", "Letra", "Edificio Padre", "Cód. Recibo"};
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
        btnAdd.addActionListener(e -> openDialog(null));
        btnEdit.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                try {
                    Inmueble inm = dao.getById(id);
                    openDialog(inm);
                } catch (SQLException ex) {
                    showError("Error al obtener inmueble: " + ex.getMessage());
                }
            } else {
                showWarning("Selecciona un inmueble para modificar.");
            }
        });
        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                String tipo = (String) tableModel.getValueAt(selectedRow, 1);
                
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "¿Estás seguro de dar de baja este inmueble?\n" +
                    (tipo.equals("EDIFICIO") ? "¡ATENCIÓN! Se eliminarán todos los pisos y locales dentro de este edificio." : ""),
                    "Confirmar Baja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        if (dao.delete(id)) {
                            showInfo("Inmueble dado de baja correctamente.");
                            refreshData();
                        }
                    } catch (SQLException ex) {
                        showError("No se pudo dar de baja el inmueble: " + ex.getMessage());
                    }
                }
            } else {
                showWarning("Selecciona un inmueble para dar de baja.");
            }
        });

        btnViewSub.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                String tipo = (String) tableModel.getValueAt(selectedRow, 1);
                if ("EDIFICIO".equals(tipo)) {
                    viewDependencies(id);
                } else {
                    showWarning("Solo se pueden ver dependencias de inmuebles tipo EDIFICIO.");
                }
            } else {
                showWarning("Selecciona un edificio.");
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
        applyFilter();
    }

    private void applyFilter() {
        String filter = cbFilter.getSelectedItem() != null ? cbFilter.getSelectedItem().toString() : "TODOS";
        tableModel.setRowCount(0);

        try {
            List<Inmueble> list = dao.getAll();
            for (Inmueble i : list) {
                if (!"TODOS".equals(filter) && !i.getTipo().equals(filter)) {
                    continue;
                }
                
                String edificioPadre = "-";
                if (i.getParentEdificioId() != null) {
                    Inmueble padre = dao.getById(i.getParentEdificioId());
                    if (padre != null) {
                        edificioPadre = padre.getDireccion() + ", No. " + padre.getNumero();
                    }
                }

                tableModel.addRow(new Object[]{
                    i.getId(),
                    i.getTipo(),
                    i.getDireccion(),
                    i.getNumero(),
                    i.getCodigoPostal(),
                    i.getPlanta() != null ? i.getPlanta() : "-",
                    i.getLetra() != null ? i.getLetra() : "-",
                    edificioPadre,
                    i.getCodigoRecibo() != null ? i.getCodigoRecibo() : "-"
                });
            }
        } catch (SQLException ex) {
            showError("Error al cargar datos: " + ex.getMessage());
        }
    }

    private void viewDependencies(int edificioId) {
        try {
            List<Inmueble> dependencias = dao.getPisosYLocalesDeEdificio(edificioId);
            if (dependencias.isEmpty()) {
                showInfo("Este edificio no tiene pisos o locales registrados.");
                return;
            }

            StringBuilder sb = new StringBuilder("Pisos y locales en este edificio:\n\n");
            for (Inmueble d : dependencias) {
                sb.append("- ").append(d.getTipo()).append(": Planta ").append(d.getPlanta()).append(", Letra ").append(d.getLetra());
                if (d.getCodigoRecibo() != null) {
                    sb.append(" (Cód. Recibo: ").append(d.getCodigoRecibo()).append(")");
                }
                sb.append("\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Dependencias del Edificio", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            showError("Error al consultar dependencias: " + ex.getMessage());
        }
    }

    private void openDialog(Inmueble inm) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            inm == null ? "Nuevo Inmueble" : "Modificar Inmueble", true);
        dlg.setSize(450, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tipo
        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Tipo:"), gbc);
        
        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"EDIFICIO", "PISO", "LOCAL"});
        gbc.gridx = 1;
        dlg.add(cbTipo, gbc);

        // Direccion
        gbc.gridx = 0; gbc.gridy = 1;
        dlg.add(new JLabel("Dirección:"), gbc);
        JTextField txtDir = new JTextField(20);
        gbc.gridx = 1;
        dlg.add(txtDir, gbc);

        // Número
        gbc.gridx = 0; gbc.gridy = 2;
        dlg.add(new JLabel("Número:"), gbc);
        JTextField txtNum = new JTextField(10);
        gbc.gridx = 1;
        dlg.add(txtNum, gbc);

        // CP
        gbc.gridx = 0; gbc.gridy = 3;
        dlg.add(new JLabel("Código Postal:"), gbc);
        JTextField txtCP = new JTextField(10);
        gbc.gridx = 1;
        dlg.add(txtCP, gbc);

        // Planta
        gbc.gridx = 0; gbc.gridy = 4;
        dlg.add(new JLabel("Planta:"), gbc);
        JTextField txtPlanta = new JTextField(10);
        gbc.gridx = 1;
        dlg.add(txtPlanta, gbc);

        // Letra
        gbc.gridx = 0; gbc.gridy = 5;
        dlg.add(new JLabel("Letra / Letra Pta:"), gbc);
        JTextField txtLetra = new JTextField(10);
        gbc.gridx = 1;
        dlg.add(txtLetra, gbc);

        // Edificio Padre
        gbc.gridx = 0; gbc.gridy = 6;
        dlg.add(new JLabel("Edificio Perteneciente:"), gbc);
        
        DefaultComboBoxModel<InmuebleComboItem> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement(new InmuebleComboItem(null)); // Ninguno
        try {
            List<Inmueble> edificios = dao.getAll();
            for (Inmueble e : edificios) {
                if ("EDIFICIO".equals(e.getTipo())) {
                    comboModel.addElement(new InmuebleComboItem(e));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al cargar edificios.");
        }
        
        JComboBox<InmuebleComboItem> cbEdificios = new JComboBox<>(comboModel);
        gbc.gridx = 1;
        dlg.add(cbEdificios, gbc);

        // Código Recibo Único
        gbc.gridx = 0; gbc.gridy = 7;
        dlg.add(new JLabel("Código Recibo Fijo:"), gbc);
        JTextField txtCodRecibo = new JTextField(15);
        gbc.gridx = 1;
        dlg.add(txtCodRecibo, gbc);

        // Habilitar campos según tipo de inmueble
        cbTipo.addActionListener(e -> {
            boolean isEdificio = "EDIFICIO".equals(cbTipo.getSelectedItem());
            txtPlanta.setEnabled(!isEdificio);
            txtLetra.setEnabled(!isEdificio);
            cbEdificios.setEnabled(!isEdificio);
            txtCodRecibo.setEnabled(!isEdificio);
        });

        // Cargar datos si se está editando
        if (inm != null) {
            cbTipo.setSelectedItem(inm.getTipo());
            cbTipo.setEnabled(false); // No cambiar tipo al modificar
            txtDir.setText(inm.getDireccion());
            txtNum.setText(inm.getNumero());
            txtCP.setText(inm.getCodigoPostal());
            txtPlanta.setText(inm.getPlanta() != null ? inm.getPlanta() : "");
            txtLetra.setText(inm.getLetra() != null ? inm.getLetra() : "");
            txtCodRecibo.setText(inm.getCodigoRecibo() != null ? inm.getCodigoRecibo() : "");

            if (inm.getParentEdificioId() != null) {
                for (int i = 0; i < comboModel.getSize(); i++) {
                    Inmueble e = comboModel.getElementAt(i).getInmueble();
                    if (e != null && e.getId() == inm.getParentEdificioId()) {
                        cbEdificios.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } else {
            // Valores iniciales por defecto para el tipo EDIFICIO
            txtPlanta.setEnabled(true);
            txtLetra.setEnabled(true);
            cbEdificios.setEnabled(true);
            txtCodRecibo.setEnabled(true);
        }

        // Botones guardar/cancelar
        JPanel btnPanel = new JPanel();
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());
        
        btnSave.addActionListener(e -> {
            if (txtDir.getText().trim().isEmpty() || txtNum.getText().trim().isEmpty() || txtCP.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Dirección, Número y Código Postal son campos obligatorios.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String tipo = cbTipo.getSelectedItem().toString();
            String dir = txtDir.getText().trim();
            String num = txtNum.getText().trim();
            String cp = txtCP.getText().trim();
            String planta = tipo.equals("EDIFICIO") ? null : txtPlanta.getText().trim();
            String letra = tipo.equals("EDIFICIO") ? null : txtLetra.getText().trim();
            
            Integer parentId = null;
            if (!tipo.equals("EDIFICIO")) {
                InmuebleComboItem item = (InmuebleComboItem) cbEdificios.getSelectedItem();
                if (item != null && item.getInmueble() != null) {
                    parentId = item.getInmueble().getId();
                }
            }
            
            String codRecibo = tipo.equals("EDIFICIO") ? null : txtCodRecibo.getText().trim();
            if (codRecibo != null && codRecibo.isEmpty()) {
                codRecibo = null;
            }

            Inmueble itemToSave = inm;
            if (itemToSave == null) {
                itemToSave = new Inmueble(0, tipo, dir, num, cp, planta, letra, parentId, codRecibo);
                try {
                    if (dao.insert(itemToSave)) {
                        showInfo("Inmueble creado con éxito.");
                        dlg.dispose();
                        refreshData();
                    }
                } catch (SQLException ex) {
                    showError("Error al guardar: " + ex.getMessage());
                }
            } else {
                itemToSave.setDireccion(dir);
                itemToSave.setNumero(num);
                itemToSave.setCodigoPostal(cp);
                itemToSave.setPlanta(planta);
                itemToSave.setLetra(letra);
                itemToSave.setParentEdificioId(parentId);
                itemToSave.setCodigoRecibo(codRecibo);
                try {
                    if (dao.update(itemToSave)) {
                        showInfo("Inmueble modificado con éxito.");
                        dlg.dispose();
                        refreshData();
                    }
                } catch (SQLException ex) {
                    showError("Error al actualizar: " + ex.getMessage());
                }
            }
        });

        // Trigger inicial del tipo
        cbTipo.getActionListeners()[0].actionPerformed(null);

        dlg.setVisible(true);
    }

    private void showInfo(String m) { JOptionPane.showMessageDialog(this, m, "Información", JOptionPane.INFORMATION_MESSAGE); }
    private void showWarning(String m) { JOptionPane.showMessageDialog(this, m, "Advertencia", JOptionPane.WARNING_MESSAGE); }
    private void showError(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }

    // Clase auxiliar para combos de inmuebles
    private static class InmuebleComboItem {
        private final Inmueble inm;
        public InmuebleComboItem(Inmueble inm) { this.inm = inm; }
        public Inmueble getInmueble() { return inm; }
        @Override
        public String toString() {
            if (inm == null) return "Ninguno (Propiedad Independiente)";
            return inm.getDireccion() + ", No. " + inm.getNumero();
        }
    }
}
