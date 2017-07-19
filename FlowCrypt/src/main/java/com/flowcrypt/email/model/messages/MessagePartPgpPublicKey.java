/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

/**
 * This class describes the public key details.
 *
 * @author Denis Bondarenko
 *         Date: 19.07.2017
 *         Time: 12:02
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePartPgpPublicKey extends MessagePart {
    private String keyWords;
    private String fingerprint;
    private String keyOwner;

    public MessagePartPgpPublicKey(String pubkey,
                                   String keyWords,
                                   String fingerprint,
                                   String keyOwner) {
        super(MessagePartType.PGP_PUBLIC_KEY, pubkey);
        this.keyWords = keyWords;
        this.fingerprint = fingerprint;
        this.keyOwner = keyOwner;
    }

    public String getKeyWords() {
        return keyWords;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getKeyOwner() {
        return keyOwner;
    }
}
