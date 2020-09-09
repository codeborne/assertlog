package org.assertlog.junit4.log4j;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.assertlog.OriginalLogsPolicy;
import org.junit.rules.ExternalResource;
import org.opentest4j.AssertionFailedError;

import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.log4j.Logger.getRootLogger;
import static org.assertlog.OriginalLogsPolicy.HIDE;
import static org.assertlog.OriginalLogsPolicy.SHOW;
import static org.junit.Assert.fail;


public class MockLogging extends ExternalResource {
  private final OriginalLogsPolicy policy;
  private final Level logLevel;
  private final Queue<LoggingEvent> loggingEvents = new ArrayDeque<>();
  private Level systemLogLevel;
  private List<Appender> appenders;
  private Appender testAppender;

  public MockLogging() {
    this(Level.INFO, SHOW);
  }

  public MockLogging(Level logLevel) {
    this(logLevel, SHOW);
  }

  public MockLogging(OriginalLogsPolicy policy) {
    this(Level.INFO, policy);
  }

  public MockLogging(Level logLevel, OriginalLogsPolicy policy) {
    this.logLevel = logLevel;
    this.policy = policy;
  }

  public void assertNoMoreLogs() {
    if (!loggingEvents.isEmpty()) {
      fail("Untested logs found: " + loggingEvents.stream()
          .map(e -> e.getLevel() + " " + e.getLoggerName() + " " + e.getMessage()).collect(joining("\n")));
    }
  }

  public void assertLogged(String loggerName, String level, String message) {
    LoggingEvent log = loggingEvents.remove();
    assertEquals(loggerName, log.getLoggerName(), () -> message);
    assertLog(log, level, message);
  }

  public void assertLogged(String level, String message) {
    LoggingEvent log = loggingEvents.remove();
    assertLog(log, level, message);
  }

  public void assertLoggedInAnyOrder(String level, String message) {
    Predicate<LoggingEvent> predicateByLevelAndMessage =
        (LoggingEvent event) ->
            event.getLevel().toString().equals(level)
                && event.getRenderedMessage().equals(message);
    boolean isLogged = loggingEvents.removeIf(predicateByLevelAndMessage);
    assertTrue(isLogged, () -> String.format("No entry was logged for level[%s] and message[%s]", level, message));
  }

  public void assertLogged(String level, Pattern pattern) {
    LoggingEvent log = loggingEvents.remove();
    assertMatches(log.getMessage(), pattern);
    assertEquals(level, log.getLevel().toString());
  }

  /**
   * Must use assertNotLogged before all assertLogged methods, as they remove elements from queue
   */
  public void assertLogged(Pattern pattern) {
    boolean anyMatch = loggingEvents.stream().anyMatch(l -> pattern.matcher((CharSequence) l.getMessage()).matches());
    assertTrue(anyMatch, () -> String.format("No entry was logged matching [%s]", pattern));
  }

  /**
   * Must use assertNotLogged before all assertLogged methods, as they remove elements from queue
   */
  public void assertNotLogged(Pattern pattern) {
    List<LoggingEvent> anyMatch = loggingEvents.stream().filter(l -> pattern.matcher((CharSequence) l.getMessage()).matches()).collect(toList());
    assertTrue(anyMatch.isEmpty(), () -> String.format("Found log entry matching [%s]: '%s'", pattern));
  }

  public void assertLogged(String level, String message, Throwable exception) {
    LoggingEvent log = loggingEvents.remove();
    assertLog(log, level, message);
    if (exception == null)
      assertNull(log.getThrowableInformation(), () -> "Cause should be null");
    else {
      assertEquals(exception.getClass(), log.getThrowableInformation().getThrowable().getClass());
      assertEquals(exception.getMessage(), log.getThrowableInformation().getThrowable().getMessage());
    }
  }

  public LoggingEvent getLoggedEvent() {
    return loggingEvents.remove();
  }

  public void clear() {
    loggingEvents.clear();
  }

  private void assertLog(LoggingEvent log, String level, String message) {
    assertEquals(message, log.getMessage());
    assertEquals(level, log.getLevel().toString());
  }

  @Override
  protected void before() {
    Logger rootLogger = getRootLogger();
    if (policy == HIDE) {
      rootLogger.removeAllAppenders();
      appenders = getCurrentAppenders(rootLogger);
    }
    systemLogLevel = rootLogger.getLevel();
    rootLogger.setLevel(logLevel);

    testAppender = new SystemConsoleAppender() {
      @Override
      public void append(LoggingEvent log) {
        loggingEvents.add(log);
      }
    };
    rootLogger.addAppender(testAppender);
  }

  @Override
  protected void after() {
    Logger rootLogger = getRootLogger();
    rootLogger.removeAppender(testAppender);
    rootLogger.setLevel(systemLogLevel);
    if (policy == HIDE) {
      for (Appender appender : appenders) {
        rootLogger.addAppender(appender);
      }
    }
  }

  private List<Appender> getCurrentAppenders(Logger logger) {
    List<Appender> result = new ArrayList<>(3);
    for (Enumeration<Appender> en = logger.getAllAppenders(); en.hasMoreElements(); ) {
      Appender appender = en.nextElement();
      result.add(appender);
    }
    return result;
  }

  private static class SystemConsoleAppender extends ConsoleAppender {
    private SystemConsoleAppender() {
      setWriter(new OutputStreamWriter(System.out, UTF_8));
      setLayout(new PatternLayout("%d{HH:mm:ss,SSS} %-5p %c{1} - %m%n"));
    }
  }

  private <T> void assertEquals(T expected, T actual) {
    assertEquals(expected, actual, () -> null);
  }

  private <T> void assertEquals(T expected, T actual, Supplier<String> message) {
    if (!expected.equals(actual)) {
      throw new AssertionFailedError(String.format("%sexpected: <%s> but was: <%s>", prefix(message.get()), expected, actual));
    }
  }

  private <T> void assertNull(T value, Supplier<String> message) {
    if (value != null) {
      throw new AssertionFailedError(prefix(message.get()) + "expected: <null> but was: <" + value + ">", null, value);
    }
  }

  private void assertTrue(boolean value, Supplier<String> message) {
    if (!value) {
      throw new AssertionFailedError(message.get(), true, false);
    }
  }

  private void assertMatches(Object value, Pattern pattern) {
    if (!pattern.matcher(value.toString()).matches()) {
      throw new AssertionFailedError(String.format("TODO"), true, false);
    }
  }

  private String prefix(String message) {
    return message == null || message.isEmpty() ? "" : message + " ==> ";
  }
}