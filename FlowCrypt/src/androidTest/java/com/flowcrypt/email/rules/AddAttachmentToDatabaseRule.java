/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 3/14/19
 * Time: 5:54 PM
 * E-mail: DenBond7@gmail.com
 */
public class AddAttachmentToDatabaseRule implements TestRule {
  private AttachmentInfo attInfo;

  public AddAttachmentToDatabaseRule(AttachmentInfo attInfo) {
    this.attInfo = attInfo;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        saveAttToDatabase();
        base.evaluate();
      }
    };
  }

  public AttachmentInfo getAttInfo() {
    return attInfo;
  }

  private void saveAttToDatabase() {
    AttachmentDaoSource source = new AttachmentDaoSource();
    source.addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(), attInfo);
  }
}