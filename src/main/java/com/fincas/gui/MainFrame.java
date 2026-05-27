package com.fincas.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainFrame extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel titleLabel;
    private boolean isDarkMode = false;

    // Paneles de secciones
    private DashboardPanel dashboardPanel;
    private InmueblesPanel inmueblesPanel;
    private InquilinosPanel inquilinosPanel;
    private AlquileresPanel alquileresPanel;
    private RecibosPanel recibosPanel;
    private ContabilidadPanel contabilidadPanel;
    private InformesPanel informesPanel;

    public MainFrame() {
        setTitle("FincaGest - Sistema de Gestión de Fincas e Inmuebles");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 650));

        initComponents();
    }

    private void initComponents() {
        // Layout principal: Sidebar a la izquierda, Contenedor a la derecha
        setLayout(new BorderLayout());

        // 1. Sidebar (Menú de Navegación Lateral)
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBackground(new Color(30, 41, 59)); // Slate 800 - Fondo oscuro moderno
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        // Título de la app en la barra lateral
        JLabel appTitle = new JLabel("🏢 FincaGest");
        appTitle.setFont(new Font("Outfit", Font.BOLD, 22));
        appTitle.setForeground(Color.WHITE);
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(appTitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel appSubtitle = new JLabel("Gestión Inmobiliaria");
        appSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        appSubtitle.setForeground(new Color(148, 163, 184)); // Slate 400
        appSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(appSubtitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 35)));

        // Botones de navegación
        JButton btnDashboard = createNavButton("📊 Dashboard", "DASHBOARD");
        JButton btnInmuebles = createNavButton("🏢 Inmuebles", "INMUEBLES");
        JButton btnInquilinos = createNavButton("👥 Inquilinos", "INQUILINOS");
        JButton btnAlquileres = createNavButton("🔑 Alquileres", "ALQUILERES");
        JButton btnRecibos = createNavButton("📄 Recibos", "RECIBOS");
        JButton btnContabilidad = createNavButton("💰 Contabilidad", "CONTABILIDAD");
        JButton btnInformes = createNavButton("📈 Informes & Listados", "INFORMES");

        sidebar.add(btnDashboard);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnInmuebles);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnInquilinos);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnAlquileres);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnRecibos);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnContabilidad);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnInformes);

        sidebar.add(Box.createVerticalGlue());

        // Botón de Alternar Tema (Claro/Oscuro) en la parte inferior
        JButton btnTheme = new JButton("🌓 Cambiar Tema");
        btnTheme.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnTheme.setForeground(Color.WHITE);
        btnTheme.setBackground(new Color(71, 85, 105)); // Slate 600
        btnTheme.setFocusPainted(false);
        btnTheme.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnTheme.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTheme.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTheme.addActionListener(e -> toggleTheme());
        sidebar.add(btnTheme);

        add(sidebar, BorderLayout.WEST);

        // 2. Encabezado de Contenido
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));
        
        titleLabel = new JLabel("📊 Resumen del Sistema (Dashboard)");
        titleLabel.setFont(new Font("Outfit", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel infoHeader = new JLabel("Secretaría de Administración");
        infoHeader.setForeground(new Color(100, 116, 139));
        infoHeader.setFont(new Font("SansSerif", Font.ITALIC, 12));
        headerPanel.add(infoHeader, BorderLayout.EAST);

        // 3. Contenedor de contenido con CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Inicialización de Paneles
        dashboardPanel = new DashboardPanel();
        inmueblesPanel = new InmueblesPanel();
        inquilinosPanel = new InquilinosPanel();
        alquileresPanel = new AlquileresPanel();
        recibosPanel = new RecibosPanel();
        contabilidadPanel = new ContabilidadPanel();
        informesPanel = new InformesPanel();

        contentPanel.add(dashboardPanel, "DASHBOARD");
        contentPanel.add(inmueblesPanel, "INMUEBLES");
        contentPanel.add(inquilinosPanel, "INQUILINOS");
        contentPanel.add(alquileresPanel, "ALQUILERES");
        contentPanel.add(recibosPanel, "RECIBOS");
        contentPanel.add(contabilidadPanel, "CONTABILIDAD");
        contentPanel.add(informesPanel, "INFORMES");

        // Panel de la derecha (Header + Contenido)
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(contentPanel, BorderLayout.CENTER);

        add(mainContainer, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setForeground(new Color(203, 213, 225)); // Slate 300
        btn.setBackground(new Color(30, 41, 59)); // Slate 800
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 45));
        btn.setPreferredSize(new Dimension(220, 45));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        // Efectos hover y click
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(51, 65, 85)); // Slate 700
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(30, 41, 59));
                btn.setForeground(new Color(203, 213, 225));
            }
        });

        btn.addActionListener(e -> {
            cardLayout.show(contentPanel, cardName);
            titleLabel.setText(text);
            refreshPanel(cardName);
        });

        return btn;
    }

    private void refreshPanel(String cardName) {
        switch (cardName) {
            case "DASHBOARD":
                dashboardPanel.refreshData();
                break;
            case "INMUEBLES":
                inmueblesPanel.refreshData();
                break;
            case "INQUILINOS":
                inquilinosPanel.refreshData();
                break;
            case "ALQUILERES":
                alquileresPanel.refreshData();
                break;
            case "RECIBOS":
                recibosPanel.refreshData();
                break;
            case "CONTABILIDAD":
                contabilidadPanel.refreshData();
                break;
            case "INFORMES":
                informesPanel.refreshData();
                break;
        }
    }

    private void toggleTheme() {
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatLightLaf());
                isDarkMode = false;
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                isDarkMode = true;
            }
            SwingUtilities.updateComponentTreeUI(this);
            SwingUtilities.updateComponentTreeUI(dashboardPanel);
        } catch (Exception ex) {
            System.err.println("Error al alternar tema.");
        }
    }
}
