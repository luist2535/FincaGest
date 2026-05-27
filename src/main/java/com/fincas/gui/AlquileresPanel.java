package com.fincas.gui;

import com.fincas.dao.AlquilerDAO;
import com.fincas.dao.InmuebleDAO;
import com.fincas.dao.InquilinoDAO;
import com.fincas.model.Inmueble;
import com.fincas.model.Inquilino;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class AlquileresPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnRent, btnVacate;
    private final AlquilerDAO alquilerDAO = new AlquilerDAO();
    private final InmuebleDAO inmuebleDAO = new InmuebleDAO();
    private final InquilinoDAO inquilinoDAO = new InquilinoDAO();

    public AlquileresPanel() {
        setLayout(new BorderLayout());
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Barra de Herramientas Superior
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        toolBar.setOpaque(false);

        btnRent = new JButton("🔑 Iniciar Alquiler");
        btnVacate = new JButton("🚪 Finalizar Alquiler (Desalquilar)");

        styleButton(btnRent, new Color(16, 185, 129), Color.WHITE); // Emerald Green
        styleButton(btnVacate, new Color(239, 68, 68), Color.WHITE); // Red

        toolBar.add(btnRent);
        toolBar.add(btnVacate);

        add(toolBar, BorderLayout.NORTH);

        // Tabla de Alquileres
        String[] columns = {"ID Contrato", "Inmueble", "Inquilino", "Fecha Inicio", "Estado"};
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
        btnRent.addActionListener(e -> openRentDialog());
        btnVacate.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String estado = (String) tableModel.getValueAt(selectedRow, 4);
                if (!"Activo".equals(estado)) {
                    showWarning("Este alquiler ya está finalizado.");
                    return;
                }
                int idInmueble = (int) tableModel.getValueAt(selectedRow, 5);
                String dniInquilino = (String) tableModel.getValueAt(selectedRow, 6);
                
                // Pedir identificación antes de desalquilar
                if (confirmarIdentificacion(dniInquilino)) {
                    openVacateDialog(idInmueble);
                }
            } else {
                showWarning("Selecciona un alquiler activo para finalizar.");
            }
        });
    }

    private boolean confirmarIdentificacion(String dniEsperado) {
        String inputDni = JOptionPane.showInputDialog(this, 
            "Identificación Requerida:\nPor favor, introduzca el DNI del inquilino para desalquilar:",
            "Confirmación de Identidad", JOptionPane.QUESTION_MESSAGE);
        
        if (inputDni == null || inputDni.trim().isEmpty()) {
            showWarning("Operación cancelada. Se requiere la identificación.");
            return false;
        }

        if (dniEsperado != null && !dniEsperado.equalsIgnoreCase(inputDni.trim())) {
            showError("El DNI introducido no coincide con el inquilino titular del contrato.");
            return false;
        }

        return true;
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
        tableModel.setRowCount(0);
        try {
            List<Object[]> list = alquilerDAO.getAlquileresDetallados();
            for (Object[] row : list) {
                tableModel.addRow(new Object[]{
                    row[0], // ID Contrato
                    row[1], // Inmueble desc
                    row[2], // Inquilino desc
                    row[3], // Fecha Inicio
                    row[4], // Estado
                    row[5], // ID Inmueble (escondido de la vista de columnas pero visible para lógica de JTable)
                    row[6]  // DNI Inquilino
                });
            }
        } catch (SQLException ex) {
            showError("Error al cargar contratos: " + ex.getMessage());
        }
    }

    private void openRentDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Iniciar Nuevo Alquiler", true);
        dlg.setSize(500, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Inmuebles Libres
        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Inmueble Disponible:"), gbc);
        
        DefaultComboBoxModel<InmuebleComboItem> comboInmModel = new DefaultComboBoxModel<>();
        try {
            List<Inmueble> libres = inmuebleDAO.getDisponiblesParaAlquiler();
            for (Inmueble i : libres) {
                comboInmModel.addElement(new InmuebleComboItem(i));
            }
        } catch (SQLException ex) {
            System.err.println("Error al obtener inmuebles libres.");
        }
        
        JComboBox<InmuebleComboItem> cbInmuebles = new JComboBox<>(comboInmModel);
        gbc.gridx = 1;
        dlg.add(cbInmuebles, gbc);

        // Inquilinos
        gbc.gridx = 0; gbc.gridy = 1;
        dlg.add(new JLabel("Inquilino:"), gbc);
        
        DefaultComboBoxModel<InquilinoComboItem> comboInqModel = new DefaultComboBoxModel<>();
        try {
            List<Inquilino> inquilinos = inquilinoDAO.getAll();
            for (Inquilino inq : inquilinos) {
                comboInqModel.addElement(new InquilinoComboItem(inq));
            }
        } catch (SQLException ex) {
            System.err.println("Error al cargar inquilinos.");
        }
        JComboBox<InquilinoComboItem> cbInquilinos = new JComboBox<>(comboInqModel);
        gbc.gridx = 1;
        dlg.add(cbInquilinos, gbc);

        // Fecha de Inicio
        gbc.gridx = 0; gbc.gridy = 2;
        dlg.add(new JLabel("Fecha de Inicio (YYYY-MM-DD):"), gbc);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JTextField txtFecha = new JTextField(sdf.format(new java.util.Date()));
        gbc.gridx = 1;
        dlg.add(txtFecha, gbc);

        // Botones
        JPanel btnPanel = new JPanel();
        JButton btnSave = new JButton("Alquilar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());
        
        btnSave.addActionListener(e -> {
            InmuebleComboItem selectedInm = (InmuebleComboItem) cbInmuebles.getSelectedItem();
            InquilinoComboItem selectedInq = (InquilinoComboItem) cbInquilinos.getSelectedItem();
            
            if (selectedInm == null || selectedInm.getInmueble() == null) {
                JOptionPane.showMessageDialog(dlg, "Debe seleccionar un inmueble libre.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selectedInq == null || selectedInq.getInquilino() == null) {
                JOptionPane.showMessageDialog(dlg, "Debe seleccionar un inquilino.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date fechaIni;
            try {
                fechaIni = new Date(sdf.parse(txtFecha.getText().trim()).getTime());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Formato de fecha inválido. Utilice AAAA-MM-DD.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Exigencia del Enunciado: Identificación obligatoria del inquilino antes de la firma
            String inputDni = JOptionPane.showInputDialog(dlg, 
                "Firma de Contrato:\nPor favor, introduzca el DNI del inquilino (" + selectedInq.getInquilino().getNombre() + ") para autorizar el alquiler:",
                "Identificación Requerida", JOptionPane.QUESTION_MESSAGE);
            
            if (inputDni == null || !selectedInq.getInquilino().getDni().equalsIgnoreCase(inputDni.trim())) {
                JOptionPane.showMessageDialog(dlg, "Identificación inválida o cancelada. El contrato no ha sido firmado.", "Error de Firma", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Ejecutar alquiler
            try {
                if (alquilerDAO.alquilar(selectedInm.getInmueble().getId(), selectedInq.getInquilino().getId(), fechaIni)) {
                    showInfo("¡Alquiler iniciado con éxito!");
                    dlg.dispose();
                    refreshData();
                }
            } catch (SQLException ex) {
                showError("No se pudo iniciar el alquiler: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    private void openVacateDialog(int idInmueble) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Finalizar Alquiler", true);
        dlg.setSize(380, 180);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dlg.add(new JLabel("Fecha de desalojo (YYYY-MM-DD):"), gbc);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JTextField txtFecha = new JTextField(sdf.format(new java.util.Date()));
        gbc.gridx = 1;
        dlg.add(txtFecha, gbc);

        JPanel btnPanel = new JPanel();
        JButton btnSave = new JButton("Desalquilar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        dlg.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            Date fechaFin;
            try {
                fechaFin = new Date(sdf.parse(txtFecha.getText().trim()).getTime());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Formato de fecha inválido. Utilice AAAA-MM-DD.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (alquilerDAO.desalquilar(idInmueble, fechaFin)) {
                    showInfo("Inmueble desalquilado correctamente.");
                    dlg.dispose();
                    refreshData();
                }
            } catch (SQLException ex) {
                showError("No se pudo desalquilar: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    private void showInfo(String m) { JOptionPane.showMessageDialog(this, m, "Información", JOptionPane.INFORMATION_MESSAGE); }
    private void showWarning(String m) { JOptionPane.showMessageDialog(this, m, "Advertencia", JOptionPane.WARNING_MESSAGE); }
    private void showError(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }

    // Clases auxiliares para combos
    private static class InmuebleComboItem {
        private final Inmueble inm;
        public InmuebleComboItem(Inmueble i) { this.inm = i; }
        public Inmueble getInmueble() { return inm; }
        @Override
        public String toString() {
            String desc = inm.getTipo() + ": " + inm.getDireccion() + ", No. " + inm.getNumero();
            if (inm.getPlanta() != null && !inm.getPlanta().isEmpty()) desc += " - Planta " + inm.getPlanta();
            if (inm.getLetra() != null && !inm.getLetra().isEmpty()) desc += ", Letra " + inm.getLetra();
            return desc;
        }
    }

    private static class InquilinoComboItem {
        private final Inquilino inq;
        public InquilinoComboItem(Inquilino i) { this.inq = i; }
        public Inquilino getInquilino() { return inq; }
        @Override
        public String toString() {
            return inq.getNombre() + " (" + inq.getDni() + ") - Garantía: " + inq.getMetodoGarantia();
        }
    }
}
