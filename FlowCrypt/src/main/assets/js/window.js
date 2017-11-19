/* Business Source License 1.0 © 2016 Tom James Holub (human@flowcrypt.com). Use limitations apply. This version will change to GPLv3 on 2021-01-01. See https://github.com/tomholub/cryptup-chrome/tree/master/src/LICENCE */

'use strict';

var window = {
  is_bare_engine: true,
  crypto: {
    getRandomValues: function (buf) {
      var ran = $_HOST_secure_random(buf.length);
      for(var i=0; i<buf.length; i++) {
        buf[i] = ran[i];
      }
    },
  },
  catcher: {
    try: function(code) {
      var self = this;
      return function () {
        try {
          return code();
        } catch(code_err) {
          self.handle_exception(code_err);
        }
      };
    },
    version: function() {
      return engine_host_version;
    },
    handle_exception: function (e) {
      $_HOST_report(true, String(e), e.stack || '', '');
    },
    report: function (name, details) {
      try {
        throw new Error(name);
      } catch(e) {
        $_HOST_report(false, String(name), e.stack, this.format_details(details));
      }
    },
    Promise: function wrapped_Promise(f) {
      var self = this;
      return new Promise(function(resolve, reject) {
        try {
          f(resolve, reject);
        } catch(e) {
          self.handle_exception(e);
          reject({code: null, message: 'Error happened, please write me at human@flowcrypt.com to fix this\n\nError: ' + e.message, internal: 'exception'});
        }
      })
    },
    format_details: function (details) {
      if(typeof details !== 'string') {
        try {
          details = JSON.stringify(details);
        } catch(stringify_error) {
          details = '(could not stringify details "' + String(details) + '" in catcher.report because: ' + stringify_error.message + ')';
        }
      }
      return details || '(no details provided)';
    }
  },
  flowcrypt_storage: {
    keys_get: function (account_email, longid) {
      return new Promise(function (resolve, reject) {
        if(typeof longid === 'undefined') {
          resolve($_HOST_storage_keys_get(account_email));
        } else {
          resolve($_HOST_storage_keys_get(account_email, longid));
        }
      });
    },
    passphrase_get: function (account_email, longid) {
      return new Promise(function (resolve, reject) {
        resolve($_HOST_storage_passphrase_get(account_email, longid));
      });
    },
  },
};

var catcher = window.catcher;

var console = {
  log: function(x) {
    $_HOST_console_log('Js.console.log: ' + console.formatter(x));
  },
  error: function(x) {
    $_HOST_console_error('Js.console.error: ' + console.formatter(x));
  },
  formatter: function(x) {
    if(typeof x === 'object') {
      return JSON.stringify(x, null, 2);
    } else {
      return String(x);
    }
  },
};

var alert = function(m) {
  $_HOST_alert(String(m));
};

var engine_host_cb_value_formatter = function(v1, v2, v3, v4, v5) {
  engine_host_cb_catcher([v1, v2, v3, v4, v5]);
};


(function() {

    // Source: http://code.google.com/p/gflot/source/browse/trunk/flot/base64.js?r=153

    /* Copyright (C) 1999 Masanao Izumo <iz@onicos.co.jp>
     * Version: 1.0
     * LastModified: Dec 25 1999
     * This library is free. You can redistribute it and/or modify it.
     */

    /*
     * Interfaces:
     * b64 = base64encode(data);
     * data = base64decode(b64);
     */

    var base64EncodeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    var base64DecodeChars = new Array(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1);

    function base64encode(str) {
        var out, i, len;
        var c1, c2, c3;

        len = str.length;
        i = 0;
        out = "";
        while(i < len) {
            c1 = str.charCodeAt(i++) & 0xff;
            if(i == len)
            {
                out += base64EncodeChars.charAt(c1 >> 2);
                out += base64EncodeChars.charAt((c1 & 0x3) << 4);
                out += "==";
                break;
            }
            c2 = str.charCodeAt(i++);
            if(i == len)
            {
                out += base64EncodeChars.charAt(c1 >> 2);
                out += base64EncodeChars.charAt(((c1 & 0x3)<< 4) | ((c2 & 0xF0) >> 4));
                out += base64EncodeChars.charAt((c2 & 0xF) << 2);
                out += "=";
                break;
            }
            c3 = str.charCodeAt(i++);
            out += base64EncodeChars.charAt(c1 >> 2);
            out += base64EncodeChars.charAt(((c1 & 0x3)<< 4) | ((c2 & 0xF0) >> 4));
            out += base64EncodeChars.charAt(((c2 & 0xF) << 2) | ((c3 & 0xC0) >>6));
            out += base64EncodeChars.charAt(c3 & 0x3F);
        }
        return out;
    }

    function base64decode(str) {
        var c1, c2, c3, c4;
        var i, len, out;

        len = str.length;
        i = 0;
        out = "";
        while(i < len) {
            /* c1 */
            do {
                c1 = base64DecodeChars[str.charCodeAt(i++) & 0xff];
            } while(i < len && c1 == -1);
            if(c1 == -1)
                break;

            /* c2 */
            do {
                c2 = base64DecodeChars[str.charCodeAt(i++) & 0xff];
            } while(i < len && c2 == -1);
            if(c2 == -1)
                break;

            out += String.fromCharCode((c1 << 2) | ((c2 & 0x30) >> 4));

            /* c3 */
            do {
                c3 = str.charCodeAt(i++) & 0xff;
                if(c3 == 61)
                    return out;
                c3 = base64DecodeChars[c3];
            } while(i < len && c3 == -1);
            if(c3 == -1)
                break;

            out += String.fromCharCode(((c2 & 0XF) << 4) | ((c3 & 0x3C) >> 2));

            /* c4 */
            do {
                c4 = str.charCodeAt(i++) & 0xff;
                if(c4 == 61)
                    return out;
                c4 = base64DecodeChars[c4];
            } while(i < len && c4 == -1);
            if(c4 == -1)
                break;
            out += String.fromCharCode(((c3 & 0x03) << 6) | c4);
        }
        return out;
    }

    var scope = (typeof window !== "undefined") ? window : self;
    if (!scope.btoa) scope.btoa = base64encode;
    if (!scope.atob) scope.atob = base64decode;

})();
