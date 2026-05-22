#!/bin/sh

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -e

dnsmasq --no-daemon --conf-file=/etc/dnsmasq.d/flowcrypt-test.conf --log-facility=- &

echo "Configuring resolver to use dnsmasq..."
printf "nameserver 127.0.0.1\n" > /etc/resolv.conf

echo "Checking dnsmasq directly..."
dig @127.0.0.1 fel.localhost +short
dig @127.0.0.1 fel.flowcrypt.test +short
dig @127.0.0.1 www.google.com +short

echo "Checking resolver..."
ping -c 1 fel.localhost
ping -c 1 fel.flowcrypt.test
ping -c 1 www.google.com

exec nginx -g 'daemon off;'
