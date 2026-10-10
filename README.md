# Raftar VPN

Free + private VPN client for Android. **Raftar** = speed — is app ki pehchan:
**Raftar Race** (servers ki live speed race, winner auto-connect) aur **Gaming Mode**
(lowest-ping server, live ping display).

- **Free Servers:** VPN Gate (University of Tsukuba) ke live public servers — poori country
  list, koi cap nahi. 100% live data, koi fake/demo server nahi.
- **Mere Servers:** apna khud ka OpenVPN server (`.ovpn` paste / file import / QR scan) —
  config sirf phone par encrypted rehti hai.
- **Roman Urdu UI** (default) + English toggle.
- **Auto-failover:** connect fail ho to aglay server par khud try karta hai.
- **No accounts, no ads, no analytics.** Privacy Policy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
  (app ke andar bhi mojood hai).

Package: `com.raftar.vpn` · minSdk 26 · targetSdk 34 · Kotlin 1.9.24 · AGP 8.5.2

## Kaise kaam karta hai

1. App kholo → **Free Servers** tab me live server list (country-wise grouped, search ke sath).
2. Har row me asal data: IP/hostname, ping (ms), speed, active sessions, uptime.
   - 🟢 green dot = reachable (is network par chal raha hai)
   - 🔴 red/dimmed = dead/blocked (recommended se bahar)
   - Long-press karke kharab server **report** karo — woh tumhare liye hamesha ke liye chhup jayega.
3. **Best Server — Auto Connect** dabao → Raftar Race (top 12 ki parallel TCP race) →
   winner par auto-connect.
4. **Gaming Mode** → sab se kam ping wala reachable server + status/notification me live ping.
5. Ya koi server tap karke select karo → **Connect**.

Pehli connect par **prominent disclosure** (Roman Urdu + English) Accept karna zaroori hai.

## Debug APK (GitHub Actions)

1. Ye repo GitHub par `RaftarVpn` naam se banao, ye files push karo (`main` branch).
2. **Actions → Build APK** workflow khud chal parega (push par + manual).
3. Run complete hone par **Artifacts → RaftarVpn-debug-apk** se `.apk` download karo,
   phone par install karo.

> Note: agar app ki structure badle (nayi service/process) to install se pehle purani
> build **uninstall** kar dena.

## Signing & Play Store upload (Roman Urdu + English)

**Step 1 — keystore banao (sirf aik dafa):**
1. GitHub repo me **Actions → Generate Release Keystore → Run workflow** dabao.
2. Run complete hone par **Artifacts → raftar-signing** download karo — is me 2 files hain:
   `keystore-base64.txt` aur `keystore-password.txt`.
3. Repo me **Settings → Secrets and variables → Actions → New repository secret** me ye 4
   secrets add karo:
   - `KEYSTORE_BASE64` → `keystore-base64.txt` ka poora content
   - `KEYSTORE_PASSWORD` → `keystore-password.txt` ka poora content
   - `KEY_ALIAS` → `raftar`
   - `KEY_PASSWORD` → `keystore-password.txt` ka poora content (same password)
4. **Artifacts delete kar do** aur `keystore-password.txt` ka backup mehfooz jagah rakho
   (ye kho gaya to future updates sign nahi ho sakengi).

**Step 2 — signed release build:**
1. **Actions → Build Release → Run workflow** dabao.
2. **Artifacts → RaftarVpn-release** se signed `.aab` (Play Store) aur `.apk` download karo.

**Step 3 — Play Console upload:**
1. Play Console me app banao, **Play App Signing ON** rakho (recommended — Google tumhari
   upload key manage karta hai).
2. Signed `.aab` ko Internal testing / Production track par upload karo.
3. Listing me **Privacy Policy** ka link do (is repo ki `PRIVACY_POLICY.md`).
4. VPN apps ke liye Play Console me **prominent disclosure** zaroori hai — ye app pehli
   connect par dikhati hai (Roman Urdu + English), iska screenshot review ke liye tayyar rakho.

**targetSdk note:** abhi `targetSdk 34` hai jo theek hai. Play Store waqt ke sath naya
targetSdk require karta hai — launch ke waqt agar Play naya version mange to
`app/build.gradle.kts` me bump karna parega (phir AGP version bhi check karna).

## Apna server (Oracle Cloud free tier)

`server-setup/` folder dekho:
- [ORACLE_SETUP_URDU.md](server-setup/ORACLE_SETUP_URDU.md) — Roman Urdu step-by-step guide
  (account → free VM → firewall → script → `.ovpn` import).
- [setup-openvpn.sh](server-setup/setup-openvpn.sh) — unattended OpenVPN server install script.

## Project structure

```
RaftarVpn/
├── app/                          # Android app (com.raftar.vpn)
│   └── src/main/java/com/raftar/vpn/
│       ├── MainActivity.kt        # permission flows, launchers (file/QR/camera)
│       ├── data/                  # VpnServer, VpnGateApi, PrivateServers, BlocklistStore
│       ├── vpn/                   # VpnConnector (tim06 OpenVPN engine wrapper)
│       └── ui/                    # App (Compose), MainViewModel, Strings (bilingual),
│                                  # GameNotification, PrivacyPolicy
├── server-setup/                  # Oracle guide + setup-openvpn.sh
├── .github/workflows/             # build-apk, build-release, generate-keystore
├── PRIVACY_POLICY.md
└── README.md
```

## Technical notes

- **OpenVPN engine:** `io.github.tim06:openvpn:1.1.3` (Apache-2.0, OpenVPN 3 C++ core,
  `libovpn3.so` for 4 ABIs) + pinned `io.github.tim06:basevpnprotocols:1.1.1`.
  AAR apni `OpenVPNService` declare karta hai — app manifest me dobara declare nahi ki.
- **Config delivery:** `OpenVPNService.startService(...)` (CONFIGURATION_KEY intent extra);
  `VpnServiceConnection` sirf bind + state callbacks ke liye.
- **VPN Gate configs:** cipher sanitize (CHACHA20-POLY1305 first), DNS inject, block-ipv6,
  TCP proto as-is (auto-rewrite nahi).
- **Data honesty:** server list 100% live API se; fetch fail = honest error + retry,
  kabhi fake list nahi. Blocked/dead = TCP reachability fail (empirical filter) +
  user blocklist.
