# RawDog

**ARM64 Linux Terminal, Sora Code Editor, DevTools Browser, and Strix Network Security Suite for Android. No root required.**

---

## Overview

RawDog is a self-contained, high-performance Linux workspace and terminal environment for Android devices (ARM64, Android 8.0+ / API 26+). It bundles a full PRoot Linux userspace, a multi-tab terminal emulator, a syntax-highlighting code editor with LSP integration, an in-app browser with network proxy support, a full storage manager, and an advanced network security auditing toolkit.

---

## Core Capabilities

### 1. Terminal & PRoot Linux Userspace
- **Rootless Linux Runtime:** Powered by PRoot on ARM64 Android with full userspace isolation.
- **Package Management:** Run standard `apt` package management to install security and development tools.
- **Session Management:** Multi-session support with background execution via persistent foreground service (`rawdog:session`).
- **Nerd Fonts:** Support for custom developer monospace typography including JetBrains Mono.
- **Red Hacker & AMOLED Palettes:** Modern dark and high-contrast colorways with customizable accent themes.

### 2. Code Editor
- **Sora Code Editor:** Fast, hardware-accelerated code editing with smooth scrolling and bracket matching.
- **TextMate Grammars:** Rich syntax highlighting for Python, JavaScript, TypeScript, C/C++, Go, Rust, Java, Kotlin, Shell, HTML, CSS, JSON, YAML, and SQL.
- **Language Server Protocol (LSP):** Real-time diagnostics, completion, and definitions via language servers (`pylsp`, `clangd`, `gopls`, `bash-language-server`, etc.).

### 3. Strix Network Security & Audit Suite
- **LAN Host Discovery:** Fast concurrent subnet scanning discovering active nodes and latency on local Wi-Fi and hotspot interfaces.
- **Multi-Threaded Port Scanner:** 16-worker thread pool auditing standard network service ports (SSH, HTTP, HTTPS, SMB, RDP, MySQL, Redis, DNS, etc.) with real-time response times.
- **Network Diagnostics:** Integrated ICMP round-trip ping latency tracker, DNS A/PTR record resolver, and HTTP server banner inspection.

### 4. DevTools Web Browser
- **App-Private Proxy:** Seamlessly route in-app HTTP/HTTPS traffic through local intercepting proxies (mitmproxy, Caido, Burp Suite) with isolated custom CA certificates.
- **Developer Inspection:** Console logging, DOM tree inspection, network request interception, and viewport emulation.

### 5. File Manager & Rices
- **Dual-Pane Storage Access:** Navigate Android internal storage, SD cards, and the PRoot Linux filesystem directly.
- **Rice Packaging:** Export and import complete workspace desktop configurations and themes (`.nsrice` packages).
- **Zero Telemetry:** 100% private and local; no analytics, no external tracking, and no third-party communications.

---

## Navigation & Workspaces

RawDog provides a streamlined navigation experience:
- **Bottom Navigation Dock:** 1-tap switching between **Terminal**, **Editor**, **Files**, **Network**, and **Hub**.
- **Workspace Navigation Drawer:** 310dp sliding drawer displaying running sessions, system state, and quick shortcuts to all workspace applications.
- **Hardware & Keyboard Support:** Full support for soft input, external keyboards, and extended shortcut bars (Ctrl, Alt, Tab, Esc, pipe, arrows).

---

## Building from Source

RawDog uses standard Android Gradle build tools:

```bash
# Verify unit tests and compilation
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```

Output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
