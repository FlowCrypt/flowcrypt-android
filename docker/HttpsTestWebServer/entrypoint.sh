#!/bin/sh

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -e

dnsmasq --no-daemon --conf-file=/etc/dnsmasq.d/flowcrypt-test.conf --log-facility=- &
exec nginx -g 'daemon off;'
