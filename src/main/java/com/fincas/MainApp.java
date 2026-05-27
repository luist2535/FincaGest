package com.fincas;

import com.fincas.db.DatabaseConnection;
import com.fincas.gui.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;

public class MainApp {
    public static void main(String[] args) {
        // Configurar tema moderno de FlatLaf
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("No se pudo inicializar FlatLaf. Usando LookAndFeel por defecto.");
        }

        // Verificar la conexión de la base de datos de manera asíncrona
        SwingUtilities.invokeLater(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Conexión exitosa, abrir ventana principal
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (SQLException e) {
                // Error de conexión (generalmente XAMPP inactivo)
                String mensaje = "No se pudo conectar a la base de datos MySQL en localhost.\n\n" +
                                "Por favor, asegúrate de que:\n" +
                                "1. XAMPP esté iniciado.\n" +
                                "2. El servicio MySQL esté activo en el puerto 3306.\n" +
                                "3. La base de datos 'fincas_db' y sus tablas hayan sido creadas (ejecuta el script 'schema.sql').\n\n" +
                                "Detalle del error:\n" + e.getMessage();
                
                JOptionPane.showMessageDialog(null, mensaje, "Error de Conexión a Base de Datos", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
