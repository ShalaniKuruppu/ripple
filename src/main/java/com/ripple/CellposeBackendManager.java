package com.ripple;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the Cellpose backend server process.
 * Handles starting, stopping, and monitoring the FastAPI backend.
 */
public class CellposeBackendManager {
    private static final String BACKEND_URL = "http://127.0.0.1:8000";
    private static final int DEFAULT_PORT = 8000;
    private static final String DEFAULT_HOST = "127.0.0.1";
    
    private Process backendProcess;
    private Thread outputReaderThread;
    private Thread errorReaderThread;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private List<String> logs = new ArrayList<>();
    private final int maxLogLines = 100;
    
    /**
     * Check if the backend is currently responding.
     */
    public boolean isBackendAvailable() {
        try {
            URL url = new URL(BACKEND_URL + "/getModels");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Start the Cellpose backend server.
     */
    public boolean startBackend() {
        if (isRunning.get()) {
            addLog("Backend is already running.");
            return true;
        }
        
        try {
            // Find the backend directory
            Path backendDir = findBackendDirectory();
            if (backendDir == null) {
                addLog("ERROR: Could not find cellpose backend directory.");
                return false;
            }
            
            addLog("Starting Cellpose backend from: " + backendDir);
            
            // Determine Python executable
            String pythonCmd = findPythonExecutable();
            if (pythonCmd == null) {
                addLog("ERROR: Could not find Python executable.");
                return false;
            }
            
            addLog("Using Python: " + pythonCmd);
            
            // Build command
            Path startScript = backendDir.resolve("cellpose backend").resolve("start_backend.py");
            if (!startScript.toFile().exists()) {
                addLog("ERROR: start_backend.py not found at: " + startScript);
                return false;
            }
            
            List<String> command = new ArrayList<>();
            command.add(pythonCmd);
            command.add(startScript.toString());
            command.add("--host");
            command.add(DEFAULT_HOST);
            command.add("--port");
            command.add(String.valueOf(DEFAULT_PORT));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(backendDir.resolve("cellpose backend").toFile());
            pb.redirectErrorStream(false);
            
            addLog("Command: " + String.join(" ", command));
            addLog("Working directory: " + pb.directory());
            
            backendProcess = pb.start();
            isRunning.set(true);
            
            // Start threads to read output
            outputReaderThread = new Thread(() -> readOutputStream(backendProcess.getInputStream()));
            errorReaderThread = new Thread(() -> readErrorStream(backendProcess.getErrorStream()));
            outputReaderThread.setDaemon(true);
            errorReaderThread.setDaemon(true);
            outputReaderThread.start();
            errorReaderThread.start();
            
            addLog("Backend process started. Waiting for server to be ready...");
            
            // Wait for backend to be ready
            boolean ready = waitForBackend(30);
            if (ready) {
                addLog("✓ Cellpose backend is ready!");
                return true;
            } else {
                addLog("WARNING: Backend started but not responding. Check logs.");
                return false;
            }
            
        } catch (Exception e) {
            addLog("ERROR starting backend: " + e.getMessage());
            e.printStackTrace();
            isRunning.set(false);
            return false;
        }
    }
    
    /**
     * Stop the backend server.
     */
    public void stopBackend() {
        if (!isRunning.get()) {
            addLog("Backend is not running.");
            return;
        }
        
        try {
            addLog("Stopping Cellpose backend...");
            
            if (backendProcess != null && backendProcess.isAlive()) {
                backendProcess.destroy();
                boolean exited = backendProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                
                if (!exited) {
                    addLog("Force killing backend process...");
                    backendProcess.destroyForcibly();
                }
            }
            
            isRunning.set(false);
            addLog("✓ Backend stopped.");
            
        } catch (Exception e) {
            addLog("ERROR stopping backend: " + e.getMessage());
        }
    }
    
    /**
     * Get the backend URL.
     */
    public String getBackendUrl() {
        return BACKEND_URL;
    }
    
    /**
     * Get backend logs.
     */
    public List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }
    
    /**
     * Clear logs.
     */
    public void clearLogs() {
        synchronized (logs) {
            logs.clear();
        }
    }
    
    /**
     * Check if backend is running.
     */
    public boolean isRunning() {
        return isRunning.get() && (backendProcess != null && backendProcess.isAlive());
    }
    
    // Private helper methods
    
    private void addLog(String message) {
        synchronized (logs) {
            logs.add(message);
            if (logs.size() > maxLogLines) {
                logs.remove(0);
            }
        }
        System.out.println("[CellposeBackend] " + message);
    }
    
    private void readOutputStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addLog("[OUT] " + line);
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                addLog("Error reading output: " + e.getMessage());
            }
        }
    }
    
    private void readErrorStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addLog("[ERR] " + line);
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                addLog("Error reading error stream: " + e.getMessage());
            }
        }
    }
    
    private boolean waitForBackend(int timeoutSeconds) {
        int attempts = timeoutSeconds * 2; // Check every 500ms
        for (int i = 0; i < attempts; i++) {
            if (isBackendAvailable()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    private Path findBackendDirectory() {
        // Start from project root and look for cellpose backend
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path backendDir = projectRoot.resolve("cellpose backend");
        
        if (backendDir.toFile().exists() && backendDir.toFile().isDirectory()) {
            return backendDir;
        }
        
        // Try parent directory
        backendDir = projectRoot.getParent().resolve("cellpose backend");
        if (backendDir.toFile().exists() && backendDir.toFile().isDirectory()) {
            return backendDir;
        }
        
        return null;
    }
    
    private String findPythonExecutable() {
        // First, try the venv_v3 Python (where packages are installed)
        Path backendDir = findBackendDirectory();
        if (backendDir != null) {
            Path venvPython = backendDir.resolve("cellpose backend").resolve("venv_v3").resolve("Scripts").resolve("python.exe");
            if (venvPython.toFile().exists()) {
                addLog("Found venv Python: " + venvPython);
                return venvPython.toString();
            }
        }
        
        // Fallback to system Python
        String[] pythonCmds = {"python", "python3", "py"};
        
        for (String cmd : pythonCmds) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    return cmd;
                }
            } catch (Exception e) {
                // Continue to next
            }
        }
        
        return null;
    }
    
    /**
     * Show a dialog with backend logs.
     */
    public void showLogsDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Cellpose Backend Logs", false);
        dialog.setSize(700, 500);
        
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        
        // Populate logs
        StringBuilder sb = new StringBuilder();
        for (String log : getLogs()) {
            sb.append(log).append("\n");
        }
        textArea.setText(sb.toString());
        textArea.setCaretPosition(textArea.getDocument().getLength());
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane, java.awt.BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            StringBuilder newSb = new StringBuilder();
            for (String log : getLogs()) {
                newSb.append(log).append("\n");
            }
            textArea.setText(newSb.toString());
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
        
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            clearLogs();
            textArea.setText("");
        });
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(refreshBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(closeBtn);
        
        dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
