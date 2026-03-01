package com.ripple.cellpose;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;
import ij.ImagePlus;
import ij.io.Opener;

/**
 * Standalone Cellpose Frontend (No Fiji/ImageJ Plugin Dependencies)
 * Connects to Cellpose Backend for cell segmentation.
 */
public class CellposeFrontendUI {
    
    private JFrame frame;
    private JLabel imageLabel;
    private JLabel statusLabel;
    private BufferedImage originalImage;
    private BufferedImage displayImage;
    private BufferedImage maskImage;
    private ImagePlus imagePlus;
    
    // Display settings
    private double zoomFactor = 1.0;
    private boolean showMask = true;
    private float maskOpacity = 0.5f;
    
    // Backend connection
    private String backendUrl;
    
    // UI Components
    private JComboBox<String> modelTypeCombo;
    private JComboBox<String> modelNameCombo;
    private JSpinner diameterSpinner;
    private JSpinner flowThresholdSpinner;
    private JSpinner cellprobThresholdSpinner;
    private JTextField channelsField;
    private JCheckBox useGpuCheckbox;
    private JCheckBox showMaskCheckbox;
    private JSlider opacitySlider;
    private JButton segmentButton;
    private JProgressBar progressBar;
    
    // Color map for mask visualization
    private Color[] colorMap;
    
    public CellposeFrontendUI(String backendUrl) {
        this.backendUrl = backendUrl;
        initializeColorMap();
    }
    
    /**
     * Initialize the UI.
     */
    public void initUI() {
        frame = new JFrame("Cellpose - Cell Segmentation");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLayout(new BorderLayout());
        
        // Create menu bar
        createMenuBar();
        
        // Create main split pane
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(320);
        
        // Left: Control panel
        JPanel controlPanel = createControlPanel();
        JScrollPane controlScroll = new JScrollPane(controlPanel);
        controlScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mainSplit.setLeftComponent(controlScroll);
        
        // Right: Image display
        JPanel displayPanel = createDisplayPanel();
        mainSplit.setRightComponent(displayPanel);
        
        frame.add(mainSplit, BorderLayout.CENTER);
        
        // Status bar
        statusLabel = new JLabel(" Ready - Load an image to start segmentation");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        frame.add(statusLabel, BorderLayout.SOUTH);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Load available models
        loadModels();
    }
    
    /**
     * Create menu bar.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Image...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openImage());
        fileMenu.add(openItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exportMaskItem = new JMenuItem("Export Mask...");
        exportMaskItem.addActionListener(e -> exportMask());
        fileMenu.add(exportMaskItem);
        
        JMenuItem exportOverlayItem = new JMenuItem("Export Overlay...");
        exportOverlayItem.addActionListener(e -> exportOverlay());
        fileMenu.add(exportOverlayItem);
        
        fileMenu.addSeparator();
        
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> frame.dispose());
        fileMenu.add(closeItem);
        
        // View menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
        zoomInItem.addActionListener(e -> zoomIn());
        viewMenu.add(zoomInItem);
        
        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOutItem.addActionListener(e -> zoomOut());
        viewMenu.add(zoomOutItem);
        
        JMenuItem zoomFitItem = new JMenuItem("Fit to Window");
        zoomFitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        zoomFitItem.addActionListener(e -> zoomFit());
        viewMenu.add(zoomFitItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        
        frame.setJMenuBar(menuBar);
    }
    
    /**
     * Create control panel.
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Model selection
        JPanel modelPanel = createSection("Model Selection");
        modelTypeCombo = new JComboBox<>(new String[]{"Cellpose3.1", "CellposeSAM"});
        modelTypeCombo.addActionListener(e -> loadModels());
        modelNameCombo = new JComboBox<>();
        
        addLabeledComponent(modelPanel, "Model Type:", modelTypeCombo);
        addLabeledComponent(modelPanel, "Model:", modelNameCombo);
        
        JButton refreshBtn = new JButton("Refresh Models");
        refreshBtn.addActionListener(e -> loadModels());
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        modelPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        modelPanel.add(refreshBtn);
        
        panel.add(modelPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Segmentation parameters
        JPanel paramPanel = createSection("Segmentation Parameters");
        
        diameterSpinner = new JSpinner(new SpinnerNumberModel(30.0, 0.0, 500.0, 5.0));
        flowThresholdSpinner = new JSpinner(new SpinnerNumberModel(0.4, 0.0, 3.0, 0.1));
        cellprobThresholdSpinner = new JSpinner(new SpinnerNumberModel(0.0, -6.0, 6.0, 0.5));
        channelsField = new JTextField("0,0", 10);
        useGpuCheckbox = new JCheckBox("Use GPU");
        
        addLabeledComponent(paramPanel, "Cell Diameter (px):", diameterSpinner);
        addLabeledComponent(paramPanel, "Flow Threshold:", flowThresholdSpinner);
        addLabeledComponent(paramPanel, "Cell Prob Threshold:", cellprobThresholdSpinner);
        addLabeledComponent(paramPanel, "Channels:", channelsField);
        paramPanel.add(useGpuCheckbox);
        
        panel.add(paramPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Display settings
        JPanel displaySettingsPanel = createSection("Display Settings");
        
        showMaskCheckbox = new JCheckBox("Show Mask Overlay", true);
        showMaskCheckbox.addActionListener(e -> {
            showMask = showMaskCheckbox.isSelected();
            updateDisplay();
        });
        displaySettingsPanel.add(showMaskCheckbox);
        
        displaySettingsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        displaySettingsPanel.add(new JLabel("Mask Opacity:"));
        opacitySlider = new JSlider(0, 100, 50);
        opacitySlider.setMajorTickSpacing(25);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        opacitySlider.addChangeListener(e -> {
            maskOpacity = opacitySlider.getValue() / 100.0f;
            updateDisplay();
        });
        displaySettingsPanel.add(opacitySlider);
        
        panel.add(displaySettingsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Run segmentation
        JPanel actionPanel = createSection("Actions");
        
        segmentButton = new JButton("Run Segmentation");
        segmentButton.setFont(new Font("Arial", Font.BOLD, 14));
        segmentButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        segmentButton.addActionListener(e -> runSegmentation());
        actionPanel.add(segmentButton);
        
        actionPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionPanel.add(progressBar);
        
        panel.add(actionPanel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    /**
     * Create display panel.
     */
    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Image display
        imageLabel = new JLabel("No image loaded", SwingConstants.CENTER);
        imageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        imageLabel.setForeground(Color.GRAY);
        
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setBackground(Color.DARK_GRAY);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Zoom controls
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton zoomInBtn = new JButton("+");
        JButton zoomOutBtn = new JButton("-");
        JButton zoomFitBtn = new JButton("Fit");
        JLabel zoomLabel = new JLabel("100%");
        
        zoomInBtn.addActionListener(e -> { zoomIn(); zoomLabel.setText(String.format("%.0f%%", zoomFactor * 100)); });
        zoomOutBtn.addActionListener(e -> { zoomOut(); zoomLabel.setText(String.format("%.0f%%", zoomFactor * 100)); });
        zoomFitBtn.addActionListener(e -> { zoomFit(); zoomLabel.setText("Fit"); });
        
        zoomPanel.add(new JLabel("Zoom:"));
        zoomPanel.add(zoomOutBtn);
        zoomPanel.add(zoomLabel);
        zoomPanel.add(zoomInBtn);
        zoomPanel.add(zoomFitBtn);
        
        panel.add(zoomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create a titled section panel.
     */
    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
    
    /**
     * Add a labeled component to a panel.
     */
    private void addLabeledComponent(JPanel panel, String label, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(150, 25));
        row.add(jLabel, BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        panel.add(row);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
    }
    
    /**
     * Initialize color map for mask visualization.
     */
    private void initializeColorMap() {
        colorMap = new Color[256];
        for (int i = 0; i < 256; i++) {
            float hue = (i * 137.508f) % 360 / 360.0f;  // Golden angle for distinct colors
            colorMap[i] = Color.getHSBColor(hue, 0.8f, 0.9f);
        }
        colorMap[0] = new Color(0, 0, 0, 0);  // Transparent background
    }
    
    /**
     * Load available models from backend.
     */
    private void loadModels() {
        SwingWorker<JSONObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JSONObject doInBackground() throws Exception {
                URL url = new URL(backendUrl + "/getModels");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return new JSONObject(response.toString());
            }
            
            @Override
            protected void done() {
                try {
                    JSONObject models = get();
                    String modelType = (String) modelTypeCombo.getSelectedItem();
                    JSONArray modelList = models.getJSONArray(modelType);
                    
                    modelNameCombo.removeAllItems();
                    for (int i = 0; i < modelList.length(); i++) {
                        modelNameCombo.addItem(modelList.getString(i));
                    }
                    statusLabel.setText(" Models loaded successfully");
                } catch (Exception e) {
                    statusLabel.setText(" Error loading models: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Open an image file.
     */
    private void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Image Files", "tif", "tiff", "png", "jpg", "jpeg", "bmp"));
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                // Try ImageJ first for better TIFF support
                Opener opener = new Opener();
                imagePlus = opener.openImage(file.getAbsolutePath());
                if (imagePlus != null) {
                    originalImage = imagePlus.getBufferedImage();
                } else {
                    // Fallback to standard ImageIO
                    originalImage = ImageIO.read(file);
                }
                
                if (originalImage != null) {
                    maskImage = null;
                    zoomFit();
                    statusLabel.setText(" Loaded: " + file.getName());
                } else {
                    JOptionPane.showMessageDialog(frame,
                        "Failed to load image: " + file.getName(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                    "Error loading image: " + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Run segmentation.
     */
    private void runSegmentation() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(frame,
                "Please load an image first.",
                "No Image", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        segmentButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("Running segmentation...");
        statusLabel.setText(" Running segmentation...");
        
        SwingWorker<BufferedImage, String> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                // Save image to temp file
                File tempFile = File.createTempFile("cellpose_input_", ".png");
                ImageIO.write(originalImage, "png", tempFile);
                
                // Build URL with query parameters
                String modelType = (String) modelTypeCombo.getSelectedItem();
                String modelName = (String) modelNameCombo.getSelectedItem();
                String diameter = String.valueOf(diameterSpinner.getValue());
                String channels = channelsField.getText();
                String useGpu = String.valueOf(useGpuCheckbox.isSelected());
                String flowThreshold = String.valueOf(flowThresholdSpinner.getValue());
                String cellprobThreshold = String.valueOf(cellprobThresholdSpinner.getValue());
                
                String urlStr = backendUrl + "/segment"
                    + "?model_type=" + java.net.URLEncoder.encode(modelType, "UTF-8")
                    + "&model_name=" + java.net.URLEncoder.encode(modelName, "UTF-8")
                    + "&diameter=" + diameter
                    + "&channels=" + java.net.URLEncoder.encode(channels, "UTF-8")
                    + "&use_gpu=" + useGpu
                    + "&flow_threshold=" + flowThreshold
                    + "&cellprob_threshold=" + cellprobThreshold;
                
                // Prepare multipart request (only for the image file)
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                String LINE_FEED = "\r\n";
                
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                
                OutputStream out = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true);
                
                // Add image file
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"").append(tempFile.getName()).append("\"").append(LINE_FEED);
                writer.append("Content-Type: image/png").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();
                
                // Write file bytes
                FileInputStream fileInput = new FileInputStream(tempFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
                fileInput.close();
                
                writer.append(LINE_FEED);
                writer.flush();
                
                // End multipart
                writer.append("--").append(boundary).append("--").append(LINE_FEED);
                writer.flush();
                writer.close();
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    // Read JSON response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder responseText = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseText.append(line);
                    }
                    reader.close();
                    
                    // Parse the JSON response
                    JSONObject dataObj = new JSONObject(responseText.toString());
                    String maskBase64 = dataObj.getString("mask_image");
                    int numCells = dataObj.getInt("num_cells");
                    
                    // Update status with cell count
                    publish("Found " + numCells + " cells");
                    
                    // Decode and create mask image
                    byte[] maskBytes = java.util.Base64.getDecoder().decode(maskBase64);
                    ByteArrayInputStream bis = new ByteArrayInputStream(maskBytes);
                    BufferedImage rawMask = ImageIO.read(bis);
                    
                    tempFile.delete();
                    return rawMask;
                } else {
                    // Read error response
                    String errorMsg = "Status: " + responseCode;
                    try {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        StringBuilder errorText = new StringBuilder();
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorText.append(line);
                        }
                        errorReader.close();
                        if (errorText.length() > 0) {
                            errorMsg += " - " + errorText.toString();
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    tempFile.delete();
                    throw new Exception("Segmentation failed: " + errorMsg);
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    statusLabel.setText(" " + msg);
                }
            }
            
            @Override
            protected void done() {
                try {
                    maskImage = get();
                    updateDisplay();
                    statusLabel.setText(" Segmentation completed");
                    progressBar.setString("Completed");
                } catch (Exception e) {
                    statusLabel.setText(" Segmentation failed: " + e.getMessage());
                    progressBar.setString("Failed");
                    JOptionPane.showMessageDialog(frame,
                        "Segmentation error: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
                progressBar.setIndeterminate(false);
                segmentButton.setEnabled(true);
            }
        };
        worker.execute();
    }
    
    private void addFormField(PrintWriter writer, String boundary, String name, String value) {
        String LINE_FEED = "\r\n";
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }
    
    /**
     * Update the display with current image and mask.
     */
    private void updateDisplay() {
        if (originalImage == null) {
            return;
        }
        
        // Create composite image
        displayImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = displayImage.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        
        // Overlay mask if available and enabled
        if (maskImage != null && showMask) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, maskOpacity));
            g.drawImage(maskImage, 0, 0, null);
        }
        g.dispose();
        
        // Apply zoom
        int width = (int) (displayImage.getWidth() * zoomFactor);
        int height = (int) (displayImage.getHeight() * zoomFactor);
        Image scaledImage = displayImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setText(null);
    }
    
    // Zoom methods
    private void zoomIn() {
        zoomFactor *= 1.25;
        updateDisplay();
    }
    
    private void zoomOut() {
        zoomFactor /= 1.25;
        updateDisplay();
    }
    
    private void zoomFit() {
        if (originalImage != null) {
            Dimension viewportSize = imageLabel.getParent().getSize();
            double scaleX = (double) viewportSize.width / originalImage.getWidth();
            double scaleY = (double) viewportSize.height / originalImage.getHeight();
            zoomFactor = Math.min(scaleX, scaleY) * 0.95;
            updateDisplay();
        }
    }
    
    // Export methods
    private void exportMask() {
        if (maskImage == null) {
            JOptionPane.showMessageDialog(frame, "No mask to export", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                ImageIO.write(maskImage, "png", file);
                statusLabel.setText(" Mask exported: " + file.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportOverlay() {
        if (displayImage == null) {
            JOptionPane.showMessageDialog(frame, "No image to export", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                ImageIO.write(displayImage, "png", file);
                statusLabel.setText(" Overlay exported: " + file.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
