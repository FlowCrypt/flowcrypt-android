#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

echo "Installing DNS tools..."
sudo apt install -y dnsmasq dnsutils

echo "Configuring dnsmasq..."
sudo tee /etc/dnsmasq.d/flowcrypt.conf >/dev/null <<'EOF'
# added by flowcrypt
listen-address=127.0.0.1
bind-interfaces

# Do not read /etc/resolv.conf to avoid recursive localhost DNS loops.
no-resolv

# Upstream DNS for public domains.
server=8.8.8.8
server=1.1.1.1

# Local test domains.
address=/test/127.0.0.1
address=/localhost/127.0.0.1
EOF

echo "Restarting dnsmasq..."
sudo systemctl restart dnsmasq

echo "DNS debug info:"
sudo ss -lunp | grep ':53' || true

echo "Checking dnsmasq directly..."
dig @127.0.0.1 fel.localhost
dig @127.0.0.1 fel.flowcrypt.test
dig @127.0.0.1 www.google.com

echo "Checking host resolver..."
ping fel.localhost -c 1
ping fel.flowcrypt.test -c 1
ping www.google.com -c 1

echo "DNS setup completed successfully."