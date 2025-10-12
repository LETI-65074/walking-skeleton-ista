package com.example.examplefeature.currency;

import com.example.examplefeature.currencyexchange.CurrencyService;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.MonetaryConversions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EcbCurrencyService implements CurrencyService {

    private final ExchangeRateProvider ecb = MonetaryConversions.getExchangeRateProvider("ECB");

    @Override
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        CurrencyUnit fromCur = Monetary.getCurrency(from);
        CurrencyUnit toCur   = Monetary.getCurrency(to);

        MonetaryAmount in = Monetary.getDefaultAmountFactory()
                .setCurrency(fromCur)
                .setNumber(amount)
                .create();

        CurrencyConversion conv = ecb.getCurrencyConversion(toCur);
        MonetaryAmount out = in.with(conv);

        return out.getNumber().numberValueExact(BigDecimal.class)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    @Override
    public BigDecimal rate(String from, String to) {
        CurrencyUnit fromCur = Monetary.getCurrency(from);
        CurrencyUnit toCur   = Monetary.getCurrency(to);
        ExchangeRate r = ecb.getExchangeRate(fromCur, toCur);
        BigDecimal factor = r.getFactor().numberValueExact(BigDecimal.class);
        return factor.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    @Override
    public Map<String, BigDecimal> rates(String base, Collection<String> targets) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (String t : targets) {
            out.put(t, rate(base, t));
        }
        return out;
    }
}
