# Test signing key (CI builds)

## `ci-debug.keystore.b64` kya hai?

Yeh **sirf test builds** ke liye ek signing key hai (base64 format me, taake GitHub API se push ho sake).

## Kyun zaroori hai?

GitHub Actions ka har runner apni **nayi debug key** banata tha. Har build alag signature se sign hoti thi, is liye:
- Nayi APK purani app ke **upar install nahi** hoti thi ("App not installed")
- User ko **uninstall** karna parta tha
- Uninstall par Android **VPN permission reset** kar deta tha → bar bar "Allow" mangta tha

Ab har build **isi ek key** se sign hoti hai (`app/build.gradle.kts` me `ciTest` signing config).
Nayi APK seedha **update** hoti hai — uninstall nahi, Allow dobara nahi.

## Ahem

- Yeh key **sirf test** ke liye hai. Repo public hai, is liye is key ko kabhi
  Play Store release ke liye istemal **nahi** karna.
- Play Store release (AAB) ke liye **alag secret key** hogi jo GitHub Secrets
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) se aayegi.
- Password: `RaftarTest2026!` | Alias: `raftardebug` | Validity: 30 saal
