/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.test;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class MimeMessage extends MeaningfulV8ObjectContainer {

    private Js js;

    public MimeMessage(V8Object o, Js js) {
        super(o);
        this.js = js;
    }

    public String getText() {
        return this.getAttributeAsString("text");
    }

    public String getHtml() {
        return this.getAttributeAsString("html");
    }

    public String getSignature() {
        return this.getAttributeAsString("signature");
    }

    public V8Object getHeaders() {
        return this.v8object.getObject("headers");
    }

    public String getStringHeader(String header_name) {
        return this.getAttributeAsString(getHeaders(), header_name);
    }

    public long getTimeHeader(String name) {
        return js.time_to_utc_timestamp(getStringHeader(name));
    }

    public MimeAddress[] getAddressHeader(String header_name) {
        V8Array addresses = this.getAttributeAsArray(getHeaders(), header_name);
        MimeAddress[] results = new MimeAddress[addresses.length()];
        for(Integer i = 0; i < addresses.length(); i++) {
            results[i] = new MimeAddress(addresses.getObject(i));
        }
        return results;
    }

    public Attachment[] getAttachments() {
        V8Array js_attachments = this.v8object.getArray("attachments");
        Attachment[] attachments = new Attachment[js_attachments.length()];
        for(Integer i = 0; i < js_attachments.length(); i++) {
            attachments[i] = new Attachment(js_attachments.getObject(i));
        }
        return attachments;
    }

}