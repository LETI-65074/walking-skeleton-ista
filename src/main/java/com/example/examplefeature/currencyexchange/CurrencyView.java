package com.example.examplefeature.currencyexchange;




import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.MonetaryConversions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Route("currency")                   // <-- unique route (no collision with home)
@PageTitle("Currency")
@Menu(order = 2, icon = "vaadin:dollar", title = "Currency")
public class CurrencyView extends Main {

    private static final List<String> POP = List.of(
            "USD","EUR","GBP","JPY","AUD","CAD","CHF","CNY","INR",
            "BRL","SEK","NOK","NZD","ZAR","SGD","HKD","MXN","TRY","KRW","PLN","DKK"
    );

    private final ExchangeRateProvider ecb = MonetaryConversions.getExchangeRateProvider("ECB");

    private final ComboBox<String> from = new ComboBox<>("From");
    private final ComboBox<String> to   = new ComboBox<>("To");
    private final NumberField amount   = new NumberField("Amount");
    private final Button convertBtn    = new Button("Convert");
    private final Span result          = new Span();

    private final Grid<Row> grid       = new Grid<>(Row.class, false);

    public CurrencyView() {
        setSizeFull();
        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        // Controls
        from.setItems(POP);
        from.setAllowCustomValue(true);
        from.setValue("USD");
        from.setWidth("12em");

        to.setItems(POP);
        to.setAllowCustomValue(true);
        to.setValue("EUR");
        to.setWidth("12em");

        //amount.setHasControls(true);
        amount.setMin(0d);
        amount.setStep(1d);
        amount.setValue(100d);
        amount.setWidth("10em");
        amount.setValueChangeMode(ValueChangeMode.EAGER);

        convertBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        convertBtn.addClickListener(e -> convert());

        HorizontalLayout controls = new HorizontalLayout(from, to, amount, convertBtn);
        //controls.setWrapMode(HorizontalLayout.WrapMode.WRAP);

        result.getStyle().set("font-weight", "1000");

        // Grid
        grid.addColumn(Row::code).setHeader("Currency").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> fmt(r.rate())).setHeader("Rate (" + from.getValue() + "→code)").setAutoWidth(true);
        grid.addColumn(r -> fmt(r.value())).setHeader("Value (code)").setAutoWidth(true);
        grid.setSizeFull();

        add(controls, result, grid);

        // initial load
        convert();
    }

    private void convert() {
        try {
            String base = or(from.getValue(), "USD");
            String dst  = or(to.getValue(), "EUR");
            BigDecimal qty = BigDecimal.valueOf(or(amount.getValue(), 0d)).setScale(4, RoundingMode.HALF_UP);

            // Compute base -> dst for the banner
            BigDecimal out = convertAmount(qty, base, dst);
            result.setText(qty.stripTrailingZeros().toPlainString() + " " + base +
                    " ≈ " + out.stripTrailingZeros().toPlainString() + " " + dst);

            // Build rows: show rate(base→code) and value(amount in base → code)
            List<Row> rows = POP.stream()
                    .sorted()
                    .map(code -> {
                        BigDecimal rate = rate(base, code);
                        BigDecimal value = convertAmount(qty, base, code);
                        return new Row(code, rate, value);
                    })
                    .collect(Collectors.toList());

            grid.getColumns().get(1).setHeader("Rate (" + base + "→code)");
            grid.getColumns().get(2).setHeader("Value (code)");
            grid.setItems(rows);

        } catch (Exception ex) {
            Notification.show("Conversion failed: " + ex.getMessage(), 4500, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            grid.setItems();
            result.setText("");
        }
    }

    // --- JavaMoney helpers (no separate service) ---

    private BigDecimal convertAmount(BigDecimal amount, String from, String to) {
        Objects.requireNonNull(amount, "amount");
        if (from.equalsIgnoreCase(to)) return scale(amount);

        CurrencyUnit fromCur = Monetary.getCurrency(from);
        CurrencyUnit toCur   = Monetary.getCurrency(to);

        MonetaryAmount in = Monetary.getDefaultAmountFactory()
                .setCurrency(fromCur)
                .setNumber(amount)
                .create();

        CurrencyConversion conv = ecb.getCurrencyConversion(toCur);
        MonetaryAmount out = in.with(conv);
        return scale(out.getNumber().numberValueExact(BigDecimal.class));
    }

    private BigDecimal rate(String from, String to) {
        if (from.equalsIgnoreCase(to)) return BigDecimal.ONE;
        CurrencyUnit fromCur = Monetary.getCurrency(from);
        CurrencyUnit toCur   = Monetary.getCurrency(to);
        ExchangeRate r = ecb.getExchangeRate(fromCur, toCur);
        return scaleRate(r.getFactor().numberValueExact(BigDecimal.class));
    }

    private static <T> T or(T v, T def) { return v != null ? v : def; }
    private static BigDecimal scale(BigDecimal n) { return n.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros(); }
    private static BigDecimal scaleRate(BigDecimal n) { return n.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros(); }
    private static String fmt(BigDecimal n) { return n.stripTrailingZeros().toPlainString(); }

    // Record for grid rows
    public record Row(String code, BigDecimal rate, BigDecimal value) { }
}
