# Oracle Cloud par apna FREE VPN Server — Step by Step (Roman Urdu)

Ye guide **Raftar VPN** ke "Mere Servers" section ke liye hai. Oracle Cloud ka **Always Free** tier
(zindagi bhar free) par apna private OpenVPN server banao ge — phir uski `.ovpn` file app me
import kar ke connect ho jao ge. Koi monthly kharcha nahi.

> Note: Oracle account banate waqt **credit/debit card** manga jata hai — sirf identity
> verification ke liye. Always Free resources par **koi charge nahi** kat-ta (sirf ek chhoti
> temporary authorization hold lagti hai jo kuch din me wapas ho jati hai).

## Step 1 — Oracle Cloud account banao

1. [oracle.com/cloud/free](https://www.oracle.com/cloud/free/) kholo → **"Start for free"** dabao.
2. Email, password, naam, address (Pakistan) likho. **Home Region** woh chuno jo tumhare sab se
   qareeb ho (jaise `ap-mumbai-1` ya `ap-singapore-1`) — isi region me VM banegi.
3. Credit/debit card add karo (verification ke liye; free tier par charge nahi).
4. Mobile number verify karo (OTP aayega). Account ban gaya!

## Step 2 — Free VM (server) launch karo

1. Oracle Cloud Console me login karo → top-left menu → **Compute → Instances** → **Create instance**.
2. Naam: `raftar-vpn` (kuch bhi).
3. **Image:** `Ubuntu 22.04` (Canonical Ubuntu 22.04 Minimal bhi chalegi).
4. **Shape:** `VM.Standard.E2.1.Micro` (Always Free eligible — 1 CPU, 1 GB RAM; VPN ke liye kaafi hai).
   - Agar Ampere ARM shape free me mil raha ho to woh bhi le sakte ho, lekin Micro sab se safe hai.
5. **Networking:** "Assign a public IPv4 address" **ON** rakho (taake phone se connect ho sako).
6. **SSH keys:** "Generate a key pair for me" chuno aur **private key download** kar lena
   (`.key` file) — isi se server me login ho ga.
7. **Create** dabao. 1–2 minute me VM **Running** ho jayegi. Iska **Public IP** note kar lo
   (Instance details page par nazar aayega).

## Step 3 — Firewall me UDP 1194 kholo

1. Instance page par → **Attached VNIC** → uski **Subnet** par click karo.
2. **Default Security List** kholo → **Add Ingress Rules**:
   - Source CIDR: `0.0.0.0/0`
   - Destination Port Range: `1194`
   - Protocol: **UDP**
   - Add karo.
3. (SSH ke liye port 22 pehle se khula hota hai.)

## Step 4 — Server par OpenVPN install karo

Apne computer se SSH karo (private key wali file ka path do):

```bash
ssh -i /path/to/ssh-key-xxxx.key ubuntu@<VM-KA-PUBLIC-IP>
```

Phir is repo ka script chalao:

```bash
curl -O https://raw.githubusercontent.com/<tumhara-username>/RaftarVpn/main/server-setup/setup-openvpn.sh
chmod +x setup-openvpn.sh
sudo ./setup-openvpn.sh
```

Script khud sab kare ga: OpenVPN + easy-rsa install, certificates banana, server config,
IP forwarding, firewall (NAT) rules. 2–4 minute lagte hain.

## Step 5 — Apne liye client `.ovpn` file banao

```bash
sudo ./setup-openvpn.sh new-client mera-phone
```

Ye `/root/mera-phone.ovpn` bana de ga. Apne computer par lao:

```bash
sudo cat /root/mera-phone.ovpn
```

Output ko copy karke apne computer par `mera-phone.ovpn` file me save kar lo
(ya `scp` se download kar lo).

## Step 6 — Raftar VPN app me import karo

1. App kholo → **"Mere Servers"** tab.
2. **"File import karo"** dabao → `mera-phone.ovpn` select karo.
   - Ya file ko kholo, text copy karo → **"Text paste karo"**.
3. Server ki row par **Connect** dabao. Pehli dafa disclosure Accept karna parega.
4. Ho gaya! Ab tumhara apna private VPN server chal raha hai.

## Aik se zyada devices?

Har device ke liye alag client banao (same server par):

```bash
sudo ./setup-openvpn.sh new-client dusra-phone
```

## Masla ho to

- **Connect nahi ho raha:** Oracle Security List me UDP 1194 ingress rule dobara check karo,
  aur server par `sudo systemctl status openvpn-server@server` dekho (active hona chahiye).
- **Script dobara chalana:** `sudo ./setup-openvpn.sh` dobara chalana safe hai (purani
  settings overwrite ho jati hain, clients dobara banane parenge).
