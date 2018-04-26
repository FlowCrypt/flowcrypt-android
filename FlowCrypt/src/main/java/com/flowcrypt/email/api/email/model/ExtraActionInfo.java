/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class describes information about incoming extra info from the intent with one of next actions:
 * <ul>
 * <li>{@link Intent#ACTION_VIEW}</li>
 * <li>{@link Intent#ACTION_SENDTO}</li>
 * <li>{@link Intent#ACTION_SEND}</li>
 * <li>{@link Intent#ACTION_SEND_MULTIPLE}</li>
 * </ul>
 *
 * @author Denis Bondarenko
 *         Date: 13.03.2018
 *         Time: 16:16
 *         E-mail: DenBond7@gmail.com
 */

public class ExtraActionInfo implements Parcelable {
    public static final Creator<ExtraActionInfo> CREATOR = new Creator<ExtraActionInfo>() {
        @Override
        public ExtraActionInfo createFromParcel(Parcel source) {
            return new ExtraActionInfo(source);
        }

        @Override
        public ExtraActionInfo[] newArray(int size) {
            return new ExtraActionInfo[size];
        }
    };
    private List<AttachmentInfo> attachmentInfoList;
    private ArrayList<String> toAddresses;
    private ArrayList<String> ccAddresses;
    private ArrayList<String> bccAddresses;
    private String subject;
    private String body;

    public ExtraActionInfo() {
    }

    public ExtraActionInfo(List<AttachmentInfo> attachmentInfoList, ArrayList<String> toAddresses, ArrayList<String>
            ccAddresses, ArrayList<String> bccAddresses, String subject, String body) {
        this.attachmentInfoList = attachmentInfoList;
        this.toAddresses = toAddresses;
        this.ccAddresses = ccAddresses;
        this.bccAddresses = bccAddresses;
        this.subject = subject;
        this.body = body;
    }

    protected ExtraActionInfo(Parcel in) {
        this.attachmentInfoList = in.createTypedArrayList(AttachmentInfo.CREATOR);
        this.toAddresses = in.createStringArrayList();
        this.ccAddresses = in.createStringArrayList();
        this.bccAddresses = in.createStringArrayList();
        this.subject = in.readString();
        this.body = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.attachmentInfoList);
        dest.writeStringList(this.toAddresses);
        dest.writeStringList(this.ccAddresses);
        dest.writeStringList(this.bccAddresses);
        dest.writeString(this.subject);
        dest.writeString(this.body);
    }

    public List<AttachmentInfo> getAttachmentInfoList() {
        return attachmentInfoList;
    }

    public void setAttachmentInfoList(List<AttachmentInfo> attachmentInfoList) {
        this.attachmentInfoList = attachmentInfoList;
    }

    public ArrayList<String> getToAddresses() {
        return toAddresses;
    }

    public void setToAddresses(ArrayList<String> toAddresses) {
        this.toAddresses = toAddresses;
    }

    public ArrayList<String> getCcAddresses() {
        return ccAddresses;
    }

    public void setCcAddresses(ArrayList<String> ccAddresses) {
        this.ccAddresses = ccAddresses;
    }

    public ArrayList<String> getBccAddresses() {
        return bccAddresses;
    }

    public void setBccAddresses(ArrayList<String> bccAddresses) {
        this.bccAddresses = bccAddresses;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public static class Builder {

        private List<AttachmentInfo> attachmentInfoList;
        private ArrayList<String> toAddresses;
        private ArrayList<String> ccAddresses;
        private ArrayList<String> bccAddresses;
        private String subject;
        private String body;
        private Parcel in;

        public Builder setAttachmentInfoList(List<AttachmentInfo> attachmentInfoList) {
            this.attachmentInfoList = attachmentInfoList;
            return this;
        }

        public Builder setToAddresses(ArrayList<String> toAddresses) {
            this.toAddresses = toAddresses;
            return this;
        }

        public Builder setCcAddresses(ArrayList<String> ccAddresses) {
            this.ccAddresses = ccAddresses;
            return this;
        }

        public Builder setBccAddresses(ArrayList<String> bccAddresses) {
            this.bccAddresses = bccAddresses;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setIn(Parcel in) {
            this.in = in;
            return this;
        }

        public ExtraActionInfo create() {
            return new ExtraActionInfo(attachmentInfoList, toAddresses, ccAddresses, bccAddresses, subject, body);
        }
    }
}
