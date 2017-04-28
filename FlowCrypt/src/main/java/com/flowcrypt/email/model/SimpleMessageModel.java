package com.flowcrypt.email.model;

import java.util.Date;

/**
 * Only for test. Will be removed later.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 11:51
 *         E-mail: DenBond7@gmail.com
 */

public class SimpleMessageModel {
    private String from;
    private String subject;
    private Date receiveDate;

    public SimpleMessageModel(String from, String subject, Date receiveDate) {
        this.from = from;
        this.subject = subject;
        this.receiveDate = receiveDate;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(Date receiveDate) {
        this.receiveDate = receiveDate;
    }
}
