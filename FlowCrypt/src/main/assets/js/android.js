/* Business Source License 1.0 © 2016-2017 FlowCrypt Limited. Use limitations apply. Contact human@flowcrypt.com */

'use strict';

(function () {

  window.tool.crypto.key.create = function(user_ids_as_pgp_contacts, num_bits, pass_phrase, callback) {
    openpgp.generateKey({
      numBits: num_bits,
      userIds: user_ids_as_pgp_contacts,
      passphrase: pass_phrase,
    }).then(function(key) {
      callback(key.privateKeyArmored);
    }).catch(function(error) {
      catcher.handle_exception(error);
    });
  };

})();