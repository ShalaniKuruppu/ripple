# RIPPLE - Video Annotation Tool for Biology

A hybrid Java/Python application for video annotation and particle tracking in biological microscopy.

## 🚀 Quick Start

### Prerequisites
- **Java 11+** (OpenJDK 17+ recommended)
- **Conda** (Miniconda or Anaconda)
- **Maven 3.8+** (needed to build the Java app)

### One-Command Installation

```bash
bash quickstart.sh
```

That's it! The script will:
1. Create a new conda environment `ripple-env` (or use existing one)
2. Install all Python dependencies (GPU or CPU automatically detected)
3. Build the Java application
4. Launch RIPPLE

### Windows
```cmd
quickstart.bat
```

## 🔨 Build & Run (Maven)

If you prefer to build/run manually (instead of using `quickstart.sh` / `quickstart.bat`), Maven produces a single shaded JAR at `target/ripple.jar`.

### Build

```bash
mvn clean package -DskipTests
```

Output:

- `target/ripple.jar`

### Run

```bash
java -jar target/ripple.jar
```

### Developer run (no packaging)

```bash
mvn exec:java
```

> Note: RIPPLE is a hybrid Java/Python app — you’ll typically still want your Conda environment set up so the Python tracking backend can run.## 🔬 Cell Segmentation (Cellpose Backend)

RIPPLE includes integrated Cellpose support for cell segmentation. The backend runs automatically when you start RIPPLE, but requires a one-time setup.

### First-Time Setup

Before running RIPPLE with cell segmentation features, set up the Cellpose environments:

**Windows:**
```cmd
cd "cellpose backend\cellpose backend"
.\setup_environments.bat
cd ..\..
```

**Linux/macOS:**
```bash
cd "cellpose backend/cellpose backend"
chmod +x setup_environments.sh
./setup_environments.sh
cd ../..
```

This creates two virtual environments:
- `venv_v3` - For Cellpose 3.1 models
- `venv_v4` - For CellposeSAM models

### Running RIPPLE with Cellpose

After the one-time setup, simply run RIPPLE from the project root:

```bash
java -jar target/ripple.jar
```

The Cellpose backend server will start automatically at `http://127.0.0.1:8000` and manage segmentation tasks in the background.

### Using Quick Start (Recommended)

Alternatively, use the quick start script which handles everything:

**Windows:**
```cmd
.\quickstart.bat
```

**Linux/macOS:**
```bash
./quickstart.sh
```

### Manual Backend Control (Advanced)

If you need to run the Cellpose backend separately:

**Windows:**
```cmd
cd "cellpose backend\cellpose backend"
.\start_backend_windows.bat
```

**Linux/macOS:**
```bash
cd "cellpose backend/cellpose backend"
./start_backend_mac.sh
```

The backend provides REST API endpoints at:
- `GET /getModels` - List available segmentation models
- `POST /segment` - Perform cell segmentation
## 🧠 Persistent Tracking Server (optional)

For iterative workflows, RIPPLE includes an optional long-running tracking server that keeps models warm in memory. The helper scripts manage the server lifecycle and forward tracking commands.

### Windows

```cmd
scripts\run_persistent_tracking.bat start
scripts\run_persistent_tracking.bat status
scripts\run_persistent_tracking.bat ping
```

### Linux/macOS

By default (Windows parity), Linux/macOS also uses TCP on `127.0.0.1:9876`.

```bash
./scripts/run_persistent_tracking.sh start
./scripts/run_persistent_tracking.sh status
./scripts/run_persistent_tracking.sh ping
```

If you prefer a Unix domain socket transport on Linux/macOS:

```bash
RIPPLE_TRANSPORT=unix SOCKET_PATH=/tmp/ripple-env.sock ./scripts/run_persistent_tracking.sh start
RIPPLE_TRANSPORT=unix SOCKET_PATH=/tmp/ripple-env.sock ./scripts/run_persistent_tracking.sh status
```

### Configuration (both)

You can override defaults via environment variables:

- `CONDA_ENV` (default: `ripple-env`)
- `MODEL_SIZE` (default: `large`)
- `SOCKET_HOST` / `SOCKET_PORT` (TCP mode)
- `SOCKET_PATH` (Unix socket mode on Linux/macOS)

## 📁 Project Structure

```
RIPPLE/
├── quickstart.sh / .bat         # One-click installer and launcher
├── pom.xml                      # Maven build configuration
│
├── src/main/
│   ├── java/com/ripple/         # Java GUI application
│   └── python/                  # Python tracking backend
│
├── cellpose backend/            # Cellpose segmentation backend
│   └── cellpose backend/
│       ├── app.py               # FastAPI backend server
│       ├── worker.py            # Segmentation worker script
│       ├── start_backend.py     # Backend launcher
│       ├── setup_environments.bat # Windows environment setup
│       ├── models/              # Cellpose model storage
│       ├── venv_v3/             # Cellpose 3.1 environment
│       └── venv_v4/             # CellposeSAM environment
│
├── requirements/                # Python dependencies
│   ├── requirements-cpu.txt     # CPU-only packages
│   └── requirements-gpu.txt     # GPU (CUDA) packages
│
├── conda/                       # Conda environment files
│   ├── environment.yml          # GPU environment (CUDA)
│   └── environment-cpu.yml      # CPU-only environment
│
└── scripts/                     # Build and utility scripts
```

## 📋 Requirements

### System Requirements
- **Java**: 11+ (17+ recommended)
- **Python**: 3.8+ (for Cellpose backend)
- **Conda**: Miniconda or Anaconda
- **Maven**: 3.8+ (for building from source)

### GPU Support

| Platform | GPU | Mode | Available Features |
|----------|-----|------|-------------------|
| Linux/Windows | NVIDIA CUDA | GPU | RAFT, LocoTrack, TrackPy, DIS |
| macOS | None | CPU | TrackPy, DIS |
| Any | None | CPU | TrackPy, DIS |

> **Note:** RAFT and LocoTrack require an NVIDIA GPU with CUDA support.
> On systems without CUDA, RIPPLE runs in CPU mode with TrackPy and DIS optical flow.

## 📖 Features

- **Video Annotation**: Load and annotate TIFF video stacks
- **Cell Segmentation**:
  - Cellpose 3.1 models
  - CellposeSAM models
  - Custom model support
  - Automatic backend management
- **Particle Tracking**:
  - RAFT optical flow (GPU only)
  - LocoTrack point tracking (GPU only)
  - TrackPy (CPU/GPU)
  - DIS optical flow (CPU)
- **Track Correction Methods**:
  - Full-Blend: Pure optical flow with bidirectional blending
  - Corridor-DP: Dynamic programming with adaptive corridor search
  - Blob-Assisted: Flow + blob detection for particle-like objects
- **Trajectory Editing**: Multi-anchor trajectory optimization
- **Export**: JSON and CSV export formats

## 🔧 Troubleshooting

### Java not found
Install OpenJDK 25:
```bash
# Ubuntu/Debian
sudo apt install openjdk-25-jdk

# macOS
brew install openjdk@25

# Windows: Download from https://www.oracle.com/java/technologies/downloads/#jdk25-windows
```

### Conda not found
Install Miniconda:
```bash
# Linux
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
bash Miniconda3-latest-Linux-x86_64.sh

# macOS
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-x86_64.sh
bash Miniconda3-latest-MacOSX-x86_64.sh
```

### GPU not detected
Ensure NVIDIA drivers and CUDA toolkit are installed:
```bash
nvidia-smi  # Should show GPU info
```

## 📄 License

MIT License. See [LICENSE](https://github.com/Le0nZim/ripple/blob/main/LICENCE).

## 🙏 Acknowledgments

- [ImageJ](https://fiji.sc/) - Image analysis platform
- [Cellpose](https://github.com/MouseLand/cellpose) - Cell segmentation
- [RAFT](https://github.com/princeton-vl/RAFT) - Optical flow
- [LocoTrack](https://github.com/cvlab-kaist/locotrack) - Point tracking
- [TrackPy](https://soft-matter.github.io/trackpy/v0.7/) - Particle tracking
- [TAP-Vid](https://github.com/google-deepmind/tapnet/blob/main/tapnet/tapvid/README.md) - Track Assist
