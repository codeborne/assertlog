# assertlog
Unit-tests for your logging

https://github.com/codeborne/assertlog

[![Build Status](https://travis-ci.org/codeborne/assertlog.svg?branch=master)](https://travis-ci.org/codeborne/assertlog)
[![Maven Central](https://img.shields.io/maven-central/v/com.codeborne/assertlog.svg)](https://search.maven.org/artifact/com.codeborne/assertlog)
[![MIT License](http://img.shields.io/badge/license-MIT-green.svg)](https://github.com/codeborne/assertlog/blob/master/LICENSE)
![Free](https://img.shields.io/badge/free-open--source-green.svg)

## What is AssertLog?

AssertLog is a library for unit-testing logging in your application.

Sometimes logging is very important, especially if it contains sensitive information, security incidents, money transfers etc.

Here is how you can use AssertLog:

```java
import org.assertlog.junit4.log4j.MockLogging;
import org.junit.Rule;
import org.junit.Test;

public class PetClinicTest {
  @Rule
  public MockLogging mockLogging = new MockLogging();

  @Test
  public void payment() {
      // make a payment
      mockLogging.assertLogged("INFO", "Buying 2 Cows for 99.66 eur");
  }
  
  @After
  public void tearDown() {
    mockLogging.assertNoMoreLogs();
  }
}
```

## What logging frameworks are supported?

We plan to support all major Java logging frameworks. Currently only Log4j and JUnit are supported. 

## License

AssertLog is open-source project, and distributed under the [MIT](http://choosealicense.com/licenses/mit/) license.
