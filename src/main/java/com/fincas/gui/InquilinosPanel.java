package com.fincas.gui;

import com.fincas.dao.InquilinoDAO;
import com.fincas.model.Inquilino;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class InquilinosPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnEdit, btnDelete, btnViewPhoto;
    private final InquilinoDAO dao = new InquilinoDAO();

    public InquilinosPanel() {
        setLayout(new BorderLayout());
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Barra de Herramientas Superior
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        toolBar.setOpaque(false);

        btnAdd = new JButton("➕ Nuevo Inquilino");
        btnEdit = new JButton("✏️ Modificar");
        btnDelete = new JButton("❌ Dar de Baja");
        btnViewPhoto = new JButton("🖼️ Ver Fotografía");

        styleButton(btnAdd, new Color(79, 70, 229), Color.WHITE);
        styleButton(btnEdit, new Color(245, 158, 11), Color.WHITE);
        styleButton(btnDelete, new Color(239, 68, 68), Color.WHITE);
        styleButton(btnViewPhoto, new Color(14, 165, 233), Color.WHITE);

        toolBar.add(btnAdd);
        toolBar.add(btnEdit);
        toolBar.add(btnDelete);
        toolBar.add(btnViewPhoto);

        add(toolBar, BorderLayout.NORTH);

        // Tabla de Inquilinos
        String[] columns = {"ID", "DNI", "Nombre Completo", "Edad", "Sexo", "Método Garantía", "Avalador", "Foto Path"};
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
        btnAdd.addActionListener(e -> {
            if (confirmarIdentificacion(null)) {
                openDialog(null);
            }
        });
        
        btnEdit.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                try {
                    Inquilino i = dao.getById(id);
                    if (confirmarIdentificacion(i.getDni())) {
                        openDialog(i);
                    }
                } catch (SQLException ex) {
                    showError("Error al consultar inquilino: " + ex.getMessage());
                }
            } else {
                showWarning("Selecciona un inquilino para modificar.");
            }
        });

        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                String dni = (String) tableModel.getValueAt(selectedRow, 1);
                
                if (confirmarIdentificacion(dni)) {
                    int confirm = JOptionPane.showConfirmDialog(this, 
                        "¿Estás seguro de dar de baja a este inquilino?\nSe disolverán todos sus contratos activos.",
                        "Confirmar Baja de Inquilino", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            if (dao.delete(id)) {
                                showInfo("Inquilino dado de baja correctamente.");
                                refreshData();
                            }
                        } catch (SQLException ex) {
                            showError("Error al dar de baja: " + ex.getMessage());
                        }
                    }
                }
            } else {
                showWarning("Selecciona un inquilino para dar de baja.");
            }
        });

        btnViewPhoto.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String path = (String) tableModel.getValueAt(selectedRow, 7);
                if (path != null && !path.isEmpty()) {
                    showPhotoDialog(path);
                } else {
                    showWarning("Este inquilino no tiene fotografía registrada.");
                }
            } else {
                showWarning("Selecciona un inquilino.");
            }
        });
    }

    // Requisito del enunciado: Identificación obligatoria del inquilino
    private boolean confirmarIdentificacion(String dniEsperado) {
        String inputDni = JOptionPane.showInputDialog(this, 
            "Identificación Requerida:\nPor favor, introduzca el DNI del inquilino para autorizar la operación:",
            "Confirmación de Identidad", JOptionPane.QUESTION_MESSAGE);
        
        if (inputDni == null || inputDni.trim().isEmpty()) {
            showWarning("Operación cancelada. Se requiere la identificación por DNI.");
            return false;
        }

        if (dniEsperado != null && !dniEsperado.equalsIgnoreCase(inputDni.trim())) {
            showError("DNI no coincide con el registro del inquilino seleccionado.");
            return false;
        }

        // Si es un inquilino nuevo, verificamos si existe en la BD o simplemente dejamos pasar el formato.
        if (dniEsperado == null) {
            // Verificar formato básico (p. ej. no vacío)
            return true;
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
            List<Inquilino> list = dao.getAll();
            for (Inquilino i : list) {
                String avaladorStr = "-";
                if (i.getAvaladorId() != null) {
                    Inquilino a = dao.getById(i.getAvaladorId());
                    if (a != null) {
                        avaladorStr = a.getNombre() + " (" + a.getDni() + ")";
                    }
                }

                tableModel.addRow(new Object[]{
                    i.getId(),
                    i.getDni(),
                    i.getNombre(),
                    i.getEdad(),
                    i.getSexo(),
                    i.getMetodoGarantia(),
                    avaladorStr,
                    i.getFotoPath() != null ? i.getFotoPath() : ""
                });
            }
        } catch (SQLException ex) {
            showError("Error al cargar datos: " + ex.getMessage());
        }
    }

    private void showPhotoDialog(String path) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Fotografía del Inquilino", true);
        dlg.setSize(350, 400);
        dlg.setLocationRelativeTo(this);
        
        File file = new File(path);
        if (!file.exists()) {
            dlg.add(new JLabel("Imagen no encontrada en la ruta: " + path, SwingConstants.CENTER));
        } else {
            ImageIcon icon = new ImageIcon(path);
            // Redimensionar imagen para que quepa en el diálogo
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(300, 320, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(scaledImg), SwingConstants.CENTER);
            dlg.add(label);
        }
        
        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(e -> dlg.dispose());
        dlg.add(btnClose, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void openDialog(Inquilino i) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            i == null ? "Nuevo Inquilino" : "Modificar Inquilino", true);
        dlg.setSize(620, 360);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(15, 15));

        // Panel de Formulario (Centro)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // DNI
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("DNI:"), gbc);
        JTextField txtDNI = new JTextField(15);
        gbc.gridx = 1;
        formPanel.add(txtDNI, gbc);

        // Nombre
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Nombre Completo:"), gbc);
        JTextField txtNombre = new JTextField(20);
        gbc.gridx = 1;
        formPanel.add(txtNombre, gbc);

        // Edad
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Edad:"), gbc);
        JSpinner txtEdad = new JSpinner(new SpinnerNumberModel(18, 18, 120, 1));
        gbc.gridx = 1;
        formPanel.add(txtEdad, gbc);

        // Sexo
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Sexo:"), gbc);
        JComboBox<String> cbSexo = new JComboBox<>(new String[]{"FEMENINO", "MASCULINO", "OTRO"});
        gbc.gridx = 1;
        formPanel.add(cbSexo, gbc);

        // Garantía
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Garantía Alquiler:"), gbc);
        JComboBox<String> cbGarantia = new JComboBox<>(new String[]{
            "NOMINA", "AVAL_BANCARIO", "CONTRATO_TRABAJO", "AVALADO_POR_OTRO"
        });
        gbc.gridx = 1;
        formPanel.add(cbGarantia, gbc);

        // Avalador
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Avalador (si aplica):"), gbc);
        DefaultComboBoxModel<InquilinoComboItem> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement(new InquilinoComboItem(null)); // Ninguno
        try {
            List<Inquilino> list = dao.getAll();
            for (Inquilino item : list) {
                if (i == null || item.getId() != i.getId()) {
                    comboModel.addElement(new InquilinoComboItem(item));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al cargar inquilinos avaladores.");
        }
        JComboBox<InquilinoComboItem> cbAvaladores = new JComboBox<>(comboModel);
        gbc.gridx = 1;
        formPanel.add(cbAvaladores, gbc);

        // Panel de Foto (Derecha)
        JPanel photoPanel = new JPanel(new BorderLayout(8, 8));
        photoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 15));
        photoPanel.setPreferredSize(new Dimension(170, 260));

        JLabel lblPreview = new JLabel();
        lblPreview.setPreferredSize(new Dimension(140, 170));
        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblPreview.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true));
        lblPreview.setCursor(new Cursor(Cursor.HAND_CURSOR));

        final String[] selectedFotoPath = new String[]{""};

        Runnable updatePreview = () -> {
            if (selectedFotoPath[0] == null || selectedFotoPath[0].isEmpty()) {
                lblPreview.setIcon(null);
                lblPreview.setText("<html><center>📷<br><b>Clic para cargar</b><br><font color='#94a6b8'>JPG/PNG</font></center></html>");
            } else {
                File f = new File(selectedFotoPath[0]);
                if (f.exists()) {
                    ImageIcon icon = new ImageIcon(selectedFotoPath[0]);
                    Image img = icon.getImage();
                    Image scaledImg = img.getScaledInstance(130, 160, Image.SCALE_SMOOTH);
                    lblPreview.setIcon(new ImageIcon(scaledImg));
                    lblPreview.setText("");
                } else {
                    lblPreview.setIcon(null);
                    lblPreview.setText("<html><center>⚠️<br>Foto no encontrada</center></html>");
                }
            }
        };

        updatePreview.run();

        Runnable selectPhotoAction = () -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Imágenes (JPG, PNG)", "jpg", "jpeg", "png"));
            int returnVal = chooser.showOpenDialog(dlg);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFotoPath[0] = chooser.getSelectedFile().getAbsolutePath();
                updatePreview.run();
            }
        };

        lblPreview.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectPhotoAction.run();
            }
        });

        JPanel pnlPhotoButtons = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton btnUpload = new JButton("Buscar");
        JButton btnClear = new JButton("Quitar");
        btnUpload.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnClear.setFont(new Font("SansSerif", Font.PLAIN, 11));

        btnUpload.addActionListener(e -> selectPhotoAction.run());
        btnClear.addActionListener(e -> {
            selectedFotoPath[0] = "";
            updatePreview.run();
        });

        pnlPhotoButtons.add(btnUpload);
        pnlPhotoButtons.add(btnClear);

        photoPanel.add(new JLabel("Fotografía:", SwingConstants.CENTER), BorderLayout.NORTH);
        photoPanel.add(lblPreview, BorderLayout.CENTER);
        photoPanel.add(pnlPhotoButtons, BorderLayout.SOUTH);

        // Habilitar/Deshabilitar avalador
        cbGarantia.addActionListener(e -> {
            boolean isAval = "AVALADO_POR_OTRO".equals(cbGarantia.getSelectedItem());
            cbAvaladores.setEnabled(isAval);
        });

        // Cargar datos
        if (i != null) {
            txtDNI.setText(i.getDni());
            txtDNI.setEditable(false);
            txtNombre.setText(i.getNombre());
            txtEdad.setValue(i.getEdad());
            cbSexo.setSelectedItem(i.getSexo());
            cbGarantia.setSelectedItem(i.getMetodoGarantia());
            selectedFotoPath[0] = i.getFotoPath() != null ? i.getFotoPath() : "";
            updatePreview.run();

            if (i.getAvaladorId() != null) {
                for (int idx = 0; idx < comboModel.getSize(); idx++) {
                    Inquilino item = comboModel.getElementAt(idx).getInquilino();
                    if (item != null && item.getId() == i.getAvaladorId()) {
                        cbAvaladores.setSelectedIndex(idx);
                        break;
                    }
                }
            }
        }

        // Trigger inicial garantía
        cbGarantia.getActionListeners()[0].actionPerformed(null);

        // Botones guardar/cancelar
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        styleButton(btnSave, new Color(79, 70, 229), Color.WHITE);
        btnCancel.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        dlg.add(formPanel, BorderLayout.CENTER);
        dlg.add(photoPanel, BorderLayout.EAST);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            String dni = txtDNI.getText().trim();
            String name = txtNombre.getText().trim();
            int edad = (int) txtEdad.getValue();
            String sexo = cbSexo.getSelectedItem().toString();
            String garantia = cbGarantia.getSelectedItem().toString();
            String fotoPath = selectedFotoPath[0].trim();

            Integer avaladorId = null;
            if ("AVALADO_POR_OTRO".equals(garantia)) {
                InquilinoComboItem selected = (InquilinoComboItem) cbAvaladores.getSelectedItem();
                if (selected == null || selected.getInquilino() == null) {
                    JOptionPane.showMessageDialog(dlg, "Debe seleccionar un avalador si el método de garantía es AVALADO POR OTRO.", "Garantía inválida", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                avaladorId = selected.getInquilino().getId();
            }

            if (dni.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "El DNI y Nombre son campos obligatorios.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Inquilino inqToSave = i;
            if (inqToSave == null) {
                inqToSave = new Inquilino(0, dni, name, edad, sexo, fotoPath, garantia, avaladorId);
                try {
                    if (dao.getByDni(dni) != null) {
                        JOptionPane.showMessageDialog(dlg, "Ya existe un inquilino registrado con este DNI.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (dao.insert(inqToSave)) {
                        showInfo("Inquilino creado correctamente.");
                        dlg.dispose();
                        refreshData();
                    }
                } catch (SQLException ex) {
                    showError("Error al guardar: " + ex.getMessage());
                }
            } else {
                inqToSave.setNombre(name);
                inqToSave.setEdad(edad);
                inqToSave.setSexo(sexo);
                inqToSave.setMetodoGarantia(garantia);
                inqToSave.setFotoPath(fotoPath.isEmpty() ? null : fotoPath);
                inqToSave.setAvaladorId(avaladorId);
                try {
                    if (dao.update(inqToSave)) {
                        showInfo("Inquilino modificado correctamente.");
                        dlg.dispose();
                        refreshData();
                    }
                } catch (SQLException ex) {
                    showError("Error al actualizar: " + ex.getMessage());
                }
            }
        });

        dlg.setVisible(true);
    }

    private void showInfo(String m) { JOptionPane.showMessageDialog(this, m, "Información", JOptionPane.INFORMATION_MESSAGE); }
    private void showWarning(String m) { JOptionPane.showMessageDialog(this, m, "Advertencia", JOptionPane.WARNING_MESSAGE); }
    private void showError(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }

    // Clase auxiliar para combos
    private static class InquilinoComboItem {
        private final Inquilino inquilino;
        public InquilinoComboItem(Inquilino i) { this.inquilino = i; }
        public Inquilino getInquilino() { return inquilino; }
        @Override
        public String toString() {
            if (inquilino == null) return "Ninguno";
            return inquilino.getNombre() + " (" + inquilino.getDni() + ")";
        }
    }
}
