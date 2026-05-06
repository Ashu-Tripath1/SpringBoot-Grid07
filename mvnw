#!/bin/sh
# ----------------------------------------------------------------------------
# Apache Maven Wrapper Script for Unix/Mac
# ----------------------------------------------------------------------------
# Tries system Maven first; if not found, instructs user to install it.

if command -v mvn > /dev/null 2>&1; then
    exec mvn "$@"
else
    echo ""
    echo "ERROR: 'mvn' (Maven) is not found on your PATH."
    echo ""
    echo "To install Maven:"
    echo "  macOS:  brew install maven"
    echo "  Ubuntu: sudo apt-get install maven"
    echo "  Or download from: https://maven.apache.org/download.cgi"
    echo ""
    exit 1
fi
