#!/usr/bin/env bash
# ==============================================================================
# Slay the Spire - macOS Controller & D-Pad Fix Uninstaller
# ==============================================================================
set -e

# Change directory to the script's folder
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================================="
echo " Slay the Spire - macOS Controller Fix Uninstaller        "
echo "=========================================================="
echo ""

# 1. Check if Slay the Spire is running
if pgrep -fi "SlayTheSpire" > /dev/null; then
    echo "⚠️  【警告 / Warning】"
    echo "Slay the Spire 正在運行中，請先完全關閉遊戲再繼續！"
    echo "Slay the Spire is currently running. Please close it first."
    echo ""
    read -rp "關閉遊戲後請按 Enter 鍵繼續... / Press Enter after closing the game..."
fi

# 2. Search for SlayTheSpire.app
FOUND_APPS=()

CANDIDATE_PATHS=(
    "$HOME/Library/Application Support/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app"
    "/Applications/SlayTheSpire.app"
)

for vol in /Volumes/*; do
    if [ -d "$vol" ] && [ "$vol" != "/Volumes/Macintosh HD" ]; then
        if [ -d "$vol/SteamLibrary/steamapps/common/SlayTheSpire/SlayTheSpire.app" ]; then
            CANDIDATE_PATHS+=("$vol/SteamLibrary/steamapps/common/SlayTheSpire/SlayTheSpire.app")
        fi
        if [ -d "$vol/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app" ]; then
            CANDIDATE_PATHS+=("$vol/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app")
        fi
    fi
done

for p in "${CANDIDATE_PATHS[@]}"; do
    if [ -d "$p" ]; then
        FOUND_APPS+=("$p")
    fi
done

TARGET_APP=""

if [ ${#FOUND_APPS[@]} -eq 1 ]; then
    TARGET_APP="${FOUND_APPS[0]}"
    echo "🔍 自動偵測到遊戲路徑 (Auto-detected game):"
    echo "   $TARGET_APP"
    echo ""
elif [ ${#FOUND_APPS[@]} -gt 1 ]; then
    echo "🔍 偵測到多個遊戲安裝路徑 (Multiple installations found):"
    for i in "${!FOUND_APPS[@]}"; do
        echo "   [$((i+1))] ${FOUND_APPS[$i]}"
    done
    echo "   [0] 自訂路徑 / Custom path"
    echo ""
    read -rp "請選擇編號 (Select number 1-${#FOUND_APPS[@]}): " CHOICE
    if [[ "$CHOICE" =~ ^[1-9][0-9]*$ ]] && [ "$CHOICE" -le "${#FOUND_APPS[@]}" ]; then
        TARGET_APP="${FOUND_APPS[$((CHOICE-1))]}"
    fi
fi

while [ -z "$TARGET_APP" ] || [ ! -d "$TARGET_APP" ]; do
    echo "請將 SlayTheSpire.app 拖曳到這個視窗中，然後按 Enter："
    echo "Please drag and drop SlayTheSpire.app into this window and press Enter:"
    read -rp "> " USER_INPUT
    USER_INPUT="${USER_INPUT%\"}"
    USER_INPUT="${USER_INPUT#\"}"
    USER_INPUT="${USER_INPUT%\'}"
    USER_INPUT="${USER_INPUT#\'}"
    USER_INPUT="$(echo "$USER_INPUT" | sed 's/\\ / /g')"
    if [ -d "$USER_INPUT" ]; then
        TARGET_APP="$USER_INPUT"
    else
        echo "❌ 無效的路徑，請重試 / Invalid path, please try again."
    fi
done

JAR_PATH="$TARGET_APP/Contents/Resources/desktop-1.0.jar"
if [ ! -f "$JAR_PATH" ]; then
    if [ -f "$TARGET_APP/Contents/Java/desktop-1.0.jar" ]; then
        JAR_PATH="$TARGET_APP/Contents/Java/desktop-1.0.jar"
    fi
fi

BAK_PATH="${JAR_PATH}.bak"

if [ ! -f "$BAK_PATH" ]; then
    echo "❌ 找不到原始備份檔 (${BAK_PATH})！"
    echo "❌ Backup file not found."
    echo "如果需要還原，可以在 Steam 中點選「驗證遊戲檔案完整性」即可重獲原始檔案。"
    echo "You can also verify integrity of game files via Steam to restore original files."
    exit 1
fi

echo "🔄 正在從備份還原原始檔案..."
cp "$BAK_PATH" "$JAR_PATH"

# Clean cache
echo "🧹 清理暫存快取 (Clearing cache)..."
rm -rf /tmp/libgdx* 2>/dev/null || true
rm -rf "$TMPDIR"libgdx* 2>/dev/null || true
find /var/folders -type d -name "*libgdx*" -exec rm -rf {} + 2>/dev/null || true

echo ""
echo "=========================================================="
echo "✅ 已成功還原為原始官方版本！ / Restored to original version!"
echo "=========================================================="
echo ""
read -rp "按 Enter 鍵關閉視窗 / Press Enter to exit..."
