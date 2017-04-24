/* Business Source License 1.0 © 2016 Tom James Holub (tom@cryptup.org). Use limitations apply. This version will change to GPLv3 on 2021-01-01. See https://github.com/tomholub/cryptup-chrome/tree/master/src/LICENCE */

'use strict';

var window = {
    crypto: {
        getRandomValues: function (buf) { // NOT SECURE - for testing only
            for(var i=0; i<buf.length; i++) {
                buf[i] = Math.round(Math.random() * 255);
            }
        },
    },
};