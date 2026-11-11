#!/data/data/com.termux/files/usr/bin/bash
# Copy to ~/.termux/boot/ after installing Termux:Boot.
# Store MC_DATABASE_URL in a private env file readable only by the Termux user.
set -e
source "$HOME/.config/multichoice/backend.env"
"$HOME/services/multichoice/backend/scripts/start.sh" >> "$HOME/multichoice-backend.log" 2>&1
