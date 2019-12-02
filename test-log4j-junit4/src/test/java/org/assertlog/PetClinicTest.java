package org.assertlog;

import org.assertlog.junit4.log4j.MockLogging;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;

public class PetClinicTest {
  @Rule
  public MockLogging mockLogging = new MockLogging(OriginalLogsPolicy.HIDE);

  private final PetClinic petClinic = new PetClinic();

  @Test
  public void info() {
    petClinic.buy("Cow", new BigDecimal("99.66"), 2);
    mockLogging.assertLogged("INFO", "Buying 2 Cows for 99.66 eur; price is 49.83");
  }

  @Test
  public void errorWithoutStacktrace() {
    petClinic.buy("Cow", new BigDecimal("99.66"), -2);
    mockLogging.assertLogged("ERROR", "Count cannot be negative: -2");
  }

  @Test
  public void errorWithStacktrace() {
    petClinic.buy("Cow", new BigDecimal("99.66"), 0);
    mockLogging.assertLogged("ERROR", "Failed to byu 0 Cows for 99.66 eur", new ArithmeticException("/ by zero"));
  }

  @After
  public void tearDown() {
    mockLogging.assertNoMoreLogs();
  }
}