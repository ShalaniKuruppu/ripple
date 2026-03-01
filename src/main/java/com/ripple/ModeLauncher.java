package com.ripple;

import com.ripple.cellpose.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Mode Selection Launcher for RIPPLE.
 * 
 * Provides a startup dialog that allows users to choose between:
 * - Cell Segmentation Tool: For segmenting and analyzing cells
 * - Video Annotation Tool: For tracking and annotating objects in videos
 */
public class ModeLauncher {
    
    private JDialog dialog;
    private String selectedMode = null;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ModeLauncher launcher = new ModeLauncher();
            launcher.showModeSelection();
        });
    }
    
    /**
     * Show the mode selection dialog and launch the selected tool.
     */
    public void showModeSelection() {
        // Create dialog
        dialog = new JDialog((Frame) null, "RIPPLE - Select Tool", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        
        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        mainPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("Welcome to RIPPLE");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Please select a tool to launch:");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setForeground(Color.DARK_GRAY);
        mainPanel.add(subtitleLabel);
        
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 1, 0, 15));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(400, 150));
        
        // Cell Segmentation Tool button
        // This launches the Cellpose Frontend (if available) with backend management
        JButton segmentationButton = createToolButton(
            "Cell Segmentation Tool",
            "Segment and analyze cells using Cellpose",
            new Color(52, 152, 219)
        );
        segmentationButton.addActionListener(e -> {
            selectedMode = "segmentation";
            dialog.dispose();
            launchCellSegmentationTool();
        });
        
        // Video Annotation button
        JButton annotationButton = createToolButton(
            "Video Annotation Tool",
            "Track and annotate objects in videos",
            new Color(46, 204, 113)
        );
        annotationButton.addActionListener(e -> {
            selectedMode = "annotation";
            dialog.dispose();
            launchVideoAnnotationTool();
        });
        
        buttonPanel.add(segmentationButton);
        buttonPanel.add(annotationButton);
        
        mainPanel.add(buttonPanel);
        
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Footer info
        JLabel infoLabel = new JLabel("You can always restart to switch tools");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoLabel.setForeground(Color.GRAY);
        mainPanel.add(infoLabel);
        
        // Add to dialog
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
    
    /**
     * Create a styled tool selection button.
     */
    private JButton createToolButton(String title, String description, Color baseColor) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(10, 5));
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        
        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setForeground(new Color(240, 240, 240));
        
        // Text panel
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        textPanel.add(descLabel);
        
        button.add(textPanel, BorderLayout.CENTER);
        
        // Hover effect
        Color hoverColor = baseColor.brighter();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }
        });
        
        return button;
    }
    
    /**
     * Launch the Cell Segmentation Tool.
     * This will launch the Cellpose Frontend with backend management.
     */
    private void launchCellSegmentationTool() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Launch the integrated Cellpose application (frontend + backend)
                launchIntegratedCellposeApplication();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Error launching Cell Segmentation Tool:\n" + e.getMessage(),
                    "Launch Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    /**
     * Launch the integrated Cellpose application (frontend + backend).
     */
    private void launchIntegratedCellposeApplication() {
        // Create a management frame for the Cellpose application
        JFrame managementFrame = new JFrame("Cellpose - Backend & Frontend Manager");
        managementFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        managementFrame.setSize(600, 400);
        managementFrame.setLayout(new BorderLayout());
        
        // Create backend manager
        CellposeBackendManager backendManager = new CellposeBackendManager();
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout(10, 10));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Cellpose Cell Segmentation");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusPanel.add(titleLabel, BorderLayout.NORTH);
        
        JTextArea statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusArea.setText("Initializing Cellpose...\n\n");
        JScrollPane scrollPane = new JScrollPane(statusArea);
        statusPanel.add(scrollPane, BorderLayout.CENTER);
        
        managementFrame.add(statusPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton startBackendBtn = new JButton("Start Backend");
        JButton stopBackendBtn = new JButton("Stop Backend");
        JButton launchFrontendBtn = new JButton("Launch Frontend");
        JButton viewLogsBtn = new JButton("View Logs");
        
        stopBackendBtn.setEnabled(false);
        launchFrontendBtn.setEnabled(false);
        
        // Start backend action
        startBackendBtn.addActionListener(e -> {
            startBackendBtn.setEnabled(false);
            statusArea.append("Starting Cellpose backend...\n");
            
            SwingWorker<Boolean, String> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    boolean success = backendManager.startBackend();
                    return success;
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            statusArea.append("✓ Backend started successfully!\n");
                            statusArea.append("\nYou can now launch the frontend.\n");
                            stopBackendBtn.setEnabled(true);
                            launchFrontendBtn.setEnabled(true);
                        } else {
                            statusArea.append("✗ Failed to start backend. Check logs.\n");
                            startBackendBtn.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        statusArea.append("✗ Error: " + ex.getMessage() + "\n");
                        startBackendBtn.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });
        
        // Stop backend action
        stopBackendBtn.addActionListener(e -> {
            backendManager.stopBackend();
            statusArea.append("Backend stopped.\n");
            startBackendBtn.setEnabled(true);
            stopBackendBtn.setEnabled(false);
            launchFrontendBtn.setEnabled(false);
        });
        
        // Launch frontend action - Now uses the standalone UI
        launchFrontendBtn.addActionListener(e -> {
            statusArea.append("Launching Cellpose Frontend...\n");
            try {
                CellposeFrontendUI frontend = new CellposeFrontendUI(backendManager.getBackendUrl());
                frontend.initUI();
                statusArea.append("✓ Frontend window opened.\n");
            } catch (Exception ex) {
                statusArea.append("✗ Error: " + ex.getMessage() + "\n");
                JOptionPane.showMessageDialog(managementFrame,
                    "Failed to launch frontend: " + ex.getMessage(),
                    "Launch Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // View logs action
        viewLogsBtn.addActionListener(e -> {
            backendManager.showLogsDialog(managementFrame);
        });
        
        buttonPanel.add(startBackendBtn);
        buttonPanel.add(stopBackendBtn);
        buttonPanel.add(launchFrontendBtn);
        buttonPanel.add(viewLogsBtn);
        
        managementFrame.add(buttonPanel, BorderLayout.SOUTH);
        
        // Window closing handler
        managementFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (backendManager.isRunning()) {
                    int result = JOptionPane.showConfirmDialog(managementFrame,
                        "Stop Cellpose backend before closing?",
                        "Backend Running",
                        JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        backendManager.stopBackend();
                    }
                }
            }
        });
        
        managementFrame.setLocationRelativeTo(null);
        managementFrame.setVisible(true);
        
        // Auto-start backend
        statusArea.append("Checking backend status...\n");
        if (backendManager.isBackendAvailable()) {
            statusArea.append("✓ Backend is already running!\n");
            statusArea.append("\nYou can now launch the frontend.\n");
            stopBackendBtn.setEnabled(true);
            launchFrontendBtn.setEnabled(true);
        } else {
            statusArea.append("Backend is offline.\n");
            statusArea.append("Click 'Start Backend' to begin.\n");
        }
    }
    
    /**
     * Launch the Video Annotation Tool.
     */
    private void launchVideoAnnotationTool() {
        SwingUtilities.invokeLater(() -> {
            try {
                VideoAnnotationTool tool = new VideoAnnotationTool();
                tool.initUI();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Error launching Video Annotation Tool:\n" + e.getMessage(),
                    "Launch Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
