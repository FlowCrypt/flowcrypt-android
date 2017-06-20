/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model;

/**
 * @author DenBond7
 *         Date: 13.05.2017
 *         Time: 15:09
 *         E-mail: DenBond7@gmail.com
 */

public enum PrivateKeySourceType {
    BACKUP("backup"),
    NEW("new"),
    IMPORT("import");

    private final String text;

    /**
     * @param text
     */
    private PrivateKeySourceType(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}
