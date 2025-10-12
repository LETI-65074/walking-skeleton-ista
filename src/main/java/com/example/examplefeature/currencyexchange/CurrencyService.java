package com.example.examplefeature.currencyexchange;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface CurrencyService {
    BigDecimal convert(BigDecimal amount, String from, String to);
    BigDecimal rate(String from, String to);
    Map<String, BigDecimal> rates(String base, Collection<String> targets);
}
