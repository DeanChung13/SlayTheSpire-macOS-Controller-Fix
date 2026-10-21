# Slay the Spire - macOS Controller & D-Pad Fix
[繁體中文](#繁體中文說明) | [English](#english-guide)

---

## 繁體中文說明

修復 macOS 版《Slay the Spire》（殺戮尖塔，包含 Steam 版與獨立版）在連接 PS5 DualSense / PS4 DualShock 手把時的三大核心痛點：

1. **十字鍵（D-pad）完全無反應**：原廠內建的 LibGDX 驅動誤將 PS5/PS4 的十字鍵（Hat Switch / Axis 6）過濾丟棄。
2. **按鍵提示顯示 Xbox 圖示**：原版程式碼未支援 PS4/PS5 圖示載入分支，修復後自動識別 DualSense / PS4 並顯示專屬 PlayStation 按鍵圖示（✕、○、◻、△、L1/R1 等）。
3. **選單／事件選項無法正常選取、macOS 鼠標反向漂移**：在涅奧（開局選獎勵）、事件對話框與設定選單中，原版程式碼試圖強制調用系統級的鼠標移動，在 macOS 下造成座標反向牽引與游標打架。

本修復包提供**一鍵自動安裝與自動還原腳本**，自動備份原檔，完全不影響 Steam 成就。

---

### 🚀 一鍵安裝步驟

1. 下載或 Clone 本專案。
2. 雙擊執行 **`install.command`**（如果系統提示安全阻擋，請在該檔案按右鍵 ->「打開」）。
3. 腳本會自動偵測您的 Slay the Spire 安裝路徑（支援內建磁碟與外接 Steam 庫），並自動完成備份與補丁注入。
4. **【重要】設定 Steam 控制器**：
   - 開啟 Steam 遊戲庫 -> 在 **Slay the Spire** 上按右鍵 -> **【內容...】(Properties)**。
   - 點擊左側選單的 **【控制器】(Controller)**。
   - 將「Slay the Spire 的覆寫」選項設定為：**【停用 Steam 輸入】(Disable Steam Input)**。
   *(說明：關閉 Steam 輸入後，macOS 系統才會將手把原生 HID 驅動直通給遊戲，享受最完美的實體按鍵與十字鍵解碼)*。
5. 接上手把，開啟遊戲即可正常暢玩！

---

### 🔄 還原原始遊戲
如果想要還原回官方原生版本：
* 雙擊執行 **`uninstall.command`** 即可立刻還原原始備份；或者在 Steam 中對遊戲點右鍵 ->「已安裝檔案」->「驗證遊戲檔案的完整性」。

---

### 🛠️ 技術修復細節 (Technical Details)
1. **LibGDX OIS C++ 驅動修正**：
   - 原廠 `libgdx-controllers-desktop64.dylib` 在 HID 枚舉函式中使用了 `cmpq $0x5, %rax`，強制過濾掉 Generic Desktop Usage > 5 的控制項，導致 Hat Switch (Usage `0x39`, Axis 6) 被拋棄。補丁將其放寬為 `cmpq $0x9, %rax`。
2. **PS5 Axis 6 八向十字鍵解碼**：
   - 在 `CInputListener` 中加入 macOS DualSense Axis 6 連續值之區間解碼（上 `-1.0`、右 `-0.43`、下 `+0.14`、左 `+0.71`）。
3. **macOS 虛擬游標解耦 (`MacInputWrapper`)**：
   - 繞過 LWJGL 在 macOS 下衝突的 `Mouse.setCursorPosition`，將搖桿導航位置直接橋接進遊戲內部的 `InputHelper.mX` / `InputHelper.mY`，徹底根除游標反向牽引與權限限制。

---

## English Guide

Fixes the two most annoying controller bugs in macOS *Slay the Spire* (both Steam and standalone releases) when using PS5 DualSense or PS4 controllers:

1. **D-Pad does not respond at all**: LibGDX's native macOS OIS driver drops the DualSense D-Pad (Hat Switch / Axis 6) due to an overly strict HID Usage filter.
2. **Broken menu navigation / Reverse mouse drift**: Selecting options in menus, Neow blessing events, or settings causes the native macOS cursor to warp reversely or fight controller inputs.

---

### 🚀 Quick Start

1. Download or clone this repository.
2. Double-click **`install.command`** (If macOS Gatekeeper warns you, right click -> Open).
3. The script will automatically locate your Steam / game installation, back up `desktop-1.0.jar` to `.bak`, and apply the fix.
4. **【Crucial】Steam Input Configuration**:
   - In Steam library, right-click **Slay the Spire** -> **Properties...**.
   - Go to **Controller**.
   - Set "Override for Slay the Spire" to **Disable Steam Input**.
   *(Disabling Steam Input allows macOS native HID to deliver low-latency direct controller input without virtual XInput translation)*.
5. Launch the game and enjoy seamless controller support!

---

### 🔄 Uninstallation
- Run **`uninstall.command`** to restore the original backup, or use Steam's "Verify integrity of game files".
