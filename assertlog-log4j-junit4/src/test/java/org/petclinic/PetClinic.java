package org.petclinic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static java.math.BigDecimal.ROUND_HALF_UP;

public class PetClinic {
  private static final Logger logger = LoggerFactory.getLogger(PetClinic.class);

  public void buy(String pet, BigDecimal amount, int count) {
    if (count < 0) {
      logger.error("Count cannot be negative: {}", count);
      return;
    }

    try {
      logger.info("Buying {} {}s for {} eur; price is {}", count, pet, amount, amount.divide(new BigDecimal(count), ROUND_HALF_UP));
    }
    catch (RuntimeException e) {
      logger.error("Failed to byu {} {}s for {} eur", count, pet, amount, e);
    }
  }
}
