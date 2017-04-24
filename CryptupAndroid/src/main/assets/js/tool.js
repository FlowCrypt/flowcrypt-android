/* Business Source License 1.0 © 2016 Tom James Holub (tom@cryptup.org). Use limitations apply. This version will change to GPLv3 on 2021-01-01. See https://github.com/tomholub/cryptup-chrome/tree/master/src/LICENCE */

'use strict';


(function ( /* ALL TOOLS */ ) {

  window.tool = {
    str: {
      is_email_valid: str_is_email_valid,
    },
    crypto: {
      key: {
        normalize: crypto_key_normalize,
      },
      message: {
        encrypt: crypto_message_encrypt,
      },
    },
    value: function(v) {
      return {
        in: function(array_or_str) { return arr_contains(array_or_str, v); } // tool.value(v).in(array_or_string)
      };
    },
    each: function(iterable, looper) {
      for (var k in iterable) {
        if(iterable.hasOwnProperty(k)){
          if(looper(k, iterable[k]) === false) {
            break;
          }
        }
      }
    },
  };

  // following are useful to run in pure non-browser V8
  var tool = window.tool;
  var openpgp = window.openpgp;

  /* tool.str */

  function str_is_email_valid(email) {
    return /[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?/i.test(email);
  }

  function arr_contains(arr, value) {
    return arr && typeof arr.indexOf === 'function' && arr.indexOf(value) !== -1;
  }

  function obj_map(original_obj, f) {
    var mapped = {};
    tool.each(original_obj, function(k, v) {
      mapped[k] = f(v);
    });
    return mapped;
  }

  /* tool.crypto.armor */

  var crypto_armor_headers_dict = {
    null: { begin: '-----BEGIN', end: '-----END' },
    public_key: { begin: '-----BEGIN PGP PUBLIC KEY BLOCK-----', end: '-----END PGP PUBLIC KEY BLOCK-----' },
    private_key: { begin: '-----BEGIN PGP PRIVATE KEY BLOCK-----', end: '-----END PGP PRIVATE KEY BLOCK-----' },
    attest_packet: { begin: '-----BEGIN ATTEST PACKET-----', end: '-----END ATTEST PACKET-----' },
    cryptup_verification: { begin: '-----BEGIN CRYPTUP VERIFICATION-----', end: '-----END CRYPTUP VERIFICATION-----' },
    signed_message: { begin: '-----BEGIN PGP SIGNED MESSAGE-----', middle: '-----BEGIN PGP SIGNATURE-----', end: '-----END PGP SIGNATURE-----' },
    signature: { begin: '-----BEGIN PGP SIGNATURE-----', end: '-----END PGP SIGNATURE-----' },
    message: { begin: '-----BEGIN PGP MESSAGE-----', end: '-----END PGP MESSAGE-----' },
  };

  function crypto_armor_headers(block_type, format) {
    if(format === 're') {
      return obj_map(crypto_armor_headers_dict[block_type || null], function (header_value) {
        return header_value.replace(/ /g, '\\\s'); // regexp match friendly
      });
    } else {
      return crypto_armor_headers_dict[block_type || null];
    }
  }

  function crypto_armor_normalize(armored, type) {
    if(tool.value(type).in(['message', 'public_key', 'private_key', 'key'])) {
      armored = armored.replace(/\r?\n/g, '\n').trim();
      var nl_2 = armored.match(/\n\n/g);
      var nl_3 = armored.match(/\n\n\n/g);
      var nl_4 = armored.match(/\n\n\n\n/g);
      var nl_6 = armored.match(/\n\n\n\n\n\n/g);
      if (nl_3 && nl_6 && nl_3.length > 1 && nl_6.length === 1) {
        return armored.replace(/\n\n\n/g, '\n'); // newlines tripled: fix
      } else if(nl_2 && nl_4 && nl_2.length > 1 && nl_4.length === 1) {
        return armored.replace(/\n\n/g, '\n'); // newlines doubled.GPA on windows does this, and sometimes message can get extracted this way from html
      }
      return armored;
    } else {
      return armored;
    }
  }

  function crypto_key_normalize(armored) {
    try {
      armored = crypto_armor_normalize(armored, 'key');
      var key;
      if(RegExp(crypto_armor_headers('public_key', 're').begin).test(armored)) {
        key = openpgp.key.readArmored(armored).keys[0];
      } else if(RegExp(crypto_armor_headers('message', 're').begin).test(armored)) {
        key = openpgp.key.Key(openpgp.message.readArmored(armored).packets);
      }
      if(key) {
        return key.armor();
      } else {
        return armored;
      }
    } catch(error) {
      console.log(error);
    }
  }

  /* tool.crypo.message */

  function crypto_message_encrypt(armored_pubkeys, data, filename, armor, callback) {
    var options = { data: data, armor: armor };
    if(filename) {
      options.filename = filename;
    }
    if(armored_pubkeys) {
      options.publicKeys = [];
      tool.each(armored_pubkeys, function (i, armored_pubkey) {
        options.publicKeys = options.publicKeys.concat(openpgp.key.readArmored(armored_pubkey).keys);
      });
    }
    openpgp.encrypt(options).then(function (result) {
      callback(result);
    }, function (error) {
      callback({error: String(error)});
    });
  }

})();
