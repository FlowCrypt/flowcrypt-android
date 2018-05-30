/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol;

import javax.mail.FetchProfile;

/**
 * This class extends {@link FetchProfile.Item} and can be used for creating a custom parameter for a fetch.
 *
 * @author Denis Bondarenko
 * Date: 25.05.2018
 * Time: 16:46
 * E-mail: DenBond7@gmail.com
 */
public class CustomFetchProfileItem extends FetchProfile.Item {

    /**
     * This item is for fetching only first 300 characters of the body<p>
     * <p>
     * Information which received helps to understand is the message has an encrypted content or not
     */
    public static final CustomFetchProfileItem BODY_FISRT_CHARACTERS =
            new CustomFetchProfileItem("BODY.PEEK[TEXT]<0.300>", "BODY.PEEK[TEXT]<0.300>");

    private String value;

    /**
     * Constructor for an item. The name is used only for debugging.
     *
     * @param name the item name
     */
    protected CustomFetchProfileItem(String name) {
        super(name);
    }

    public CustomFetchProfileItem(String name, String value) {
        super(name);
        this.value = value;
    }

    @Override
    public String toString() {
        return "CustomFetchProfileItem{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }

    public String getValue() {
        return value;
    }

}
