# Raftar VPN — Privacy Policy

**Last updated: October 2026**

## 1. No accounts, no personal data

Raftar VPN requires no sign-up and collects no name, email, phone number, or any
personally identifiable information.

## 2. No browsing logs

The app never records, stores, or transmits your browsing history, visited domains,
DNS lookups, or traffic contents. There is no analytics SDK and no advertising SDK
in the app — nothing to report back to.

## 3. How the VPN works

When you connect, Android routes your internet traffic through the selected VPN server
using an encrypted OpenVPN tunnel. This is the entire function of the app.

## 4. Free servers (VPN Gate)

The free server list comes from the VPN Gate Academic Experiment Project (University
of Tsukuba, Japan). These servers are run by independent volunteers around the world.
Volunteer operators may keep their own operational connection logs according to their
configurations — see the official policy at https://www.vpngate.net/en/about_abuse.aspx.
Raftar VPN is an independent client and is not affiliated with the University of Tsukuba.

## 5. Your own servers ("Mere Servers")

Private .ovpn configurations you import (paste, file, or QR) are stored ONLY on your
device, encrypted with EncryptedSharedPreferences (AES-256). They are never uploaded
anywhere.

## 6. Permissions used

- **VPN (VpnService):** to establish the encrypted tunnel — core functionality.
- **Internet / network state:** to fetch the server list and connect.
- **Notifications:** to show VPN status and Gaming Mode ping.
- **Camera (optional):** only when you choose "Scan QR" to import your own server config.

## 7. Changes

If this policy changes, the updated text will ship inside the app and in this repository.

**Contact:** open an issue on the GitHub repository.
