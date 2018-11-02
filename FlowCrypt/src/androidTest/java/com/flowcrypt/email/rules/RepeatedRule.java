/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Denis Bondarenko
 * Date: 24.02.2018
 * Time: 17:28
 * E-mail: DenBond7@gmail.com
 */

public class RepeatedRule implements TestRule {

  @Override
  public Statement apply(Statement statement, Description description) {
    Statement result = statement;
    Repeat repeat = description.getAnnotation(Repeat.class);
    if (repeat != null) {
      int times = repeat.value();
      result = new RepeatStatement(statement, times);
    }
    return result;
  }

  private static class RepeatStatement extends Statement {
    private final Statement statement;
    private final int repeat;

    public RepeatStatement(Statement statement, int repeat) {
      this.statement = statement;
      this.repeat = repeat;
    }

    @Override
    public void evaluate() throws Throwable {
      for (int i = 0; i < repeat; i++) {
        statement.evaluate();
      }
    }
  }
}
