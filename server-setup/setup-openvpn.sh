#!/bin/bash
#
# Raftar VPN — OpenVPN server setup (unattended)
# Ubuntu 22.04 par root me chalao:  sudo ./setup-openvpn.sh
# Naya client banana:                sudo ./setup-openvpn.sh new-client <name>
#
# Karta kya hai:
#   - openvpn + easy-rsa install
#   - easy-rsa 3.x PKI (CA, server cert, DH, tls-crypt key) --batch, nopass
#   - /etc/openvpn/server/server.conf (UDP 1194, tun, AES-256-GCM, redirect-gateway)
#   - net.ipv4.ip_forward + iptables MASQUERADE (NAT) + persist
#   - openvpn-server@server systemd service enable + start
#
set -e

if [ "$EUID" -ne 0 ]; then
    echo "ERROR: root (sudo) me chalao: sudo $0"
    exit 1
fi

PORT=1194
PROTO=udp
EASYRSA_DIR=/etc/openvpn/easy-rsa
SERVER_CN=server

command -v ip >/dev/null 2>&1 || { echo "ERROR: 'ip' command nahi mila"; exit 1; }

install_server() {
    echo "==> Packages install ho rahe hain..."
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
        openvpn easy-rsa iptables iptables-persistent curl >/dev/null

    echo "==> PKI bana raha hoon (easy-rsa 3.x)..."
    mkdir -p "$EASYRSA_DIR"
    if [ ! -f "$EASYRSA_DIR/easyrsa" ]; then
        cp -r /usr/share/easy-rsa/* "$EASYRSA_DIR/"
    fi
    cd "$EASYRSA_DIR"

    if [ ! -d pki ]; then
        ./easyrsa init-pki >/dev/null 2>&1
    fi

    export EASYRSA_BATCH=1
    if [ ! -f pki/ca.crt ]; then
        EASYRSA_REQ_CN="RaftarVPN-CA" ./easyrsa --batch build-ca nopass >/dev/null 2>&1
    fi
    if [ ! -f "pki/issued/${SERVER_CN}.crt" ]; then
        ./easyrsa --batch build-server-full "$SERVER_CN" nopass >/dev/null 2>&1
    fi
    if [ ! -f pki/dh.pem ]; then
        ./easyrsa gen-dh >/dev/null 2>&1
    fi
    if [ ! -f ta.key ]; then
        openvpn --genkey secret ta.key
    fi
    unset EASYRSA_BATCH

    echo "==> server.conf likh raha hoon..."
    mkdir -p /etc/openvpn/server
    cat > /etc/openvpn/server/server.conf <<EOF
port $PORT
proto $PROTO
dev tun
ca $EASYRSA_DIR/pki/ca.crt
cert $EASYRSA_DIR/pki/issued/${SERVER_CN}.crt
key $EASYRSA_DIR/pki/private/${SERVER_CN}.key
dh $EASYRSA_DIR/pki/dh.pem
tls-crypt $EASYRSA_DIR/ta.key
server 10.8.0.0 255.255.255.0
topology subnet
push "redirect-gateway def1 bypass-dhcp"
push "dhcp-option DNS 1.1.1.1"
push "dhcp-option DNS 8.8.8.8"
keepalive 10 120
cipher AES-256-GCM
persist-key
persist-tun
user nobody
group nogroup
status /var/log/openvpn-status.log
verb 3
explicit-exit-notify 1
EOF
    chmod 600 /etc/openvpn/server/server.conf

    echo "==> IP forwarding + NAT..."
    echo 'net.ipv4.ip_forward=1' > /etc/sysctl.d/99-raftar-vpn.conf
    sysctl -q -p /etc/sysctl.d/99-raftar-vpn.conf

    IFACE=$(ip route | awk '/^default/ {print $5; exit}')
    if [ -z "$IFACE" ]; then
        echo "ERROR: default network interface nahi mili"
        exit 1
    fi
    # Duplicate rule se bachne ke liye pehle delete (fail ho to ignore), phir add.
    iptables -t nat -D POSTROUTING -s 10.8.0.0/24 -o "$IFACE" -j MASQUERADE 2>/dev/null || true
    iptables -t nat -A POSTROUTING -s 10.8.0.0/24 -o "$IFACE" -j MASQUERADE
    # OpenVPN port inbound (Oracle Security List me bhi kholna zaroori hai!)
    iptables -D INPUT -p udp --dport "$PORT" -j ACCEPT 2>/dev/null || true
    iptables -A INPUT -p udp --dport "$PORT" -j ACCEPT
    netfilter-persistent save >/dev/null 2>&1 || true

    echo "==> OpenVPN service start..."
    systemctl enable --now openvpn-server@server >/dev/null 2>&1

    echo ""
    echo "OK! OpenVPN server chal raha hai (UDP $PORT)."
    echo "Client banao:  sudo $0 new-client <name>"
    echo "(Oracle Cloud Security List me UDP $PORT inbound rule kholna mat bhoolo!)"
}

new_client() {
    NAME="$1"
    if [ -z "$NAME" ]; then
        echo "Usage: sudo $0 new-client <name>"
        exit 1
    fi
    # Naam me sirf safe characters
    if ! echo "$NAME" | grep -qE '^[A-Za-z0-9._-]+$'; then
        echo "ERROR: naam me sirf A-Z a-z 0-9 . _ - istemal karo"
        exit 1
    fi

    cd "$EASYRSA_DIR"
    export EASYRSA_BATCH=1
    ./easyrsa --batch build-client-full "$NAME" nopass >/dev/null 2>&1
    unset EASYRSA_BATCH

    # Server ka public IP (arg se ya auto-detect)
    SERVER_IP="$2"
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP=$(curl -s --max-time 10 ifconfig.me || true)
    fi
    if [ -z "$SERVER_IP" ]; then
        echo "ERROR: server ka public IP maloom na ho saka."
        echo "Dobara chalao: sudo $0 new-client $NAME <SERVER_PUBLIC_IP>"
        exit 1
    fi

    OUTFILE="/root/${NAME}.ovpn"
    {
        echo "client"
        echo "dev tun"
        echo "proto $PROTO"
        echo "remote $SERVER_IP $PORT"
        echo "resolv-retry infinite"
        echo "nobind"
        echo "persist-key"
        echo "persist-tun"
        echo "remote-cert-tls server"
        echo "cipher AES-256-GCM"
        echo "verb 3"
        echo "<ca>"
        cat "$EASYRSA_DIR/pki/ca.crt"
        echo "</ca>"
        echo "<cert>"
        awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/' "$EASYRSA_DIR/pki/issued/${NAME}.crt"
        echo "</cert>"
        echo "<key>"
        cat "$EASYRSA_DIR/pki/private/${NAME}.key"
        echo "</key>"
        echo "<tls-crypt>"
        cat "$EASYRSA_DIR/ta.key"
        echo "</tls-crypt>"
    } > "$OUTFILE"
    chmod 600 "$OUTFILE"

    echo ""
    echo "OK! Client file ban gayi: $OUTFILE"
    echo "Ise apne phone/computer par lao aur Raftar VPN app ke 'Mere Servers' me import karo."
}

case "${1:-install}" in
    install|"")
        install_server
        ;;
    new-client)
        new_client "$2" "$3"
        ;;
    *)
        echo "Usage:"
        echo "  sudo $0              # server install"
        echo "  sudo $0 new-client <name> [server-public-ip]"
        exit 1
        ;;
esac
