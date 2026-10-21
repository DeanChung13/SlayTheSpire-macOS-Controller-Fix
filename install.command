#!/usr/bin/env bash
# ==============================================================================
# Slay the Spire - macOS Controller & D-Pad Fix Installer
# ==============================================================================
set -e

# Change directory to the script's folder
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================================="
echo "  Slay the Spire - macOS Controller & D-Pad Fix Installer "
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

# Known paths
CANDIDATE_PATHS=(
    "$HOME/Library/Application Support/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app"
    "/Applications/SlayTheSpire.app"
)

# Search external volumes
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

# Fallback: Ask user to drag & drop
while [ -z "$TARGET_APP" ] || [ ! -d "$TARGET_APP" ]; do
    echo "請將 SlayTheSpire.app 拖曳到這個視窗中，然後按 Enter："
    echo "Please drag and drop SlayTheSpire.app into this window and press Enter:"
    read -rp "> " USER_INPUT
    # Strip quotes if dragged from Finder
    USER_INPUT="${USER_INPUT%\"}"
    USER_INPUT="${USER_INPUT#\"}"
    USER_INPUT="${USER_INPUT%\'}"
    USER_INPUT="${USER_INPUT#\'}"
    # Unescape spaces
    USER_INPUT="$(echo "$USER_INPUT" | sed 's/\\ / /g')"
    if [ -d "$USER_INPUT" ]; then
        TARGET_APP="$USER_INPUT"
    else
        echo "❌ 無效的路徑，請重試 / Invalid path, please try again."
    fi
done

JAR_PATH="$TARGET_APP/Contents/Resources/desktop-1.0.jar"
if [ ! -f "$JAR_PATH" ]; then
    # Some wrappers put it in Contents/Java
    if [ -f "$TARGET_APP/Contents/Java/desktop-1.0.jar" ]; then
        JAR_PATH="$TARGET_APP/Contents/Java/desktop-1.0.jar"
    else
        echo "❌ 找不到 desktop-1.0.jar！請確認該 app 是完整的 Slay the Spire。"
        echo "❌ desktop-1.0.jar not found inside app bundle."
        exit 1
    fi
fi

echo "📦 目標 JAR: $JAR_PATH"
echo ""

# 3. Create Backup
BAK_PATH="${JAR_PATH}.bak"
if [ ! -f "$BAK_PATH" ]; then
    echo "🛡️  正在建立原檔備份 (Creating backup):"
    echo "   -> $BAK_PATH"
    cp "$JAR_PATH" "$BAK_PATH"
else
    echo "ℹ️  已存在原始備份 (Backup already exists):"
    echo "   -> $BAK_PATH"
fi

# 4. Inject Patch
echo "⚡ 正在注入手把與十字鍵修復補丁 (Injecting fix patch)..."

cd "$SCRIPT_DIR/patch/classes"
zip -r "$JAR_PATH" com > /dev/null

cd "$SCRIPT_DIR/patch/native"
zip "$JAR_PATH" libgdx-controllers-desktop64.dylib > /dev/null

# 5. Clear libgdx cache in /var/folders & /tmp
echo "🧹 清理暫存快取 (Clearing stale libgdx cache)..."
rm -rf /tmp/libgdx* 2>/dev/null || true
rm -rf "$TMPDIR"libgdx* 2>/dev/null || true
find /var/folders -type d -name "*libgdx*" -exec rm -rf {} + 2>/dev/null || true

echo ""
echo "=========================================================="
echo "🎉 修復補丁安裝成功！ / Installation Complete!"
echo "=========================================================="
echo ""
echo "💡【重要提醒 / Important Note for Steam Players】"
echo "為了讓 macOS 原生讀取 PS5/PS4 手把並使用完整優化："
echo "1. 打開 Steam -> 在遊戲庫中對 Slay the Spire 按右鍵 ->【內容】(Properties)"
echo "2. 點選左側【控制器】(Controller)"
echo "3. 將「Slay the Spire 的覆寫」改為：【停用 Steam 輸入】(Disable Steam Input)"
echo "4. 接上 DualSense / 手把啟動遊戲即可享受完美的十字鍵與游標操作！"
echo ""
echo "如需還原，隨時執行 uninstall.command 即可。"
echo ""
read -rp "按 Enter 鍵關閉視窗 / Press Enter to exit..."
