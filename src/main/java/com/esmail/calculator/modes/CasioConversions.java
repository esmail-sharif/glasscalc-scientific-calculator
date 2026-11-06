package com.esmail.calculator.modes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The 40 metric conversion commands of CASIO fx-991ES PLUS / fx-570ES PLUS (Shift + 8 [CONV]).
 * Formula data is based on NIST Special Publication 811 (2008).
 */
public final class CasioConversions {
    @FunctionalInterface
    public interface Converter {
        double convert(double value);
    }

    public record ConversionEntry(int number, String fromUnit, String toUnit, String label, Converter converter) {}

    private static final Map<Integer, ConversionEntry> CONVERSIONS;

    static {
        Map<Integer, ConversionEntry> map = new LinkedHashMap<>();

        // Length
        map.put(1, new ConversionEntry(1, "in", "cm", "in ▶ cm", v -> v * 2.54));
        map.put(2, new ConversionEntry(2, "cm", "in", "cm ▶ in", v -> v / 2.54));
        map.put(3, new ConversionEntry(3, "ft", "m", "ft ▶ m", v -> v * 0.3048));
        map.put(4, new ConversionEntry(4, "m", "ft", "m ▶ ft", v -> v / 0.3048));
        map.put(5, new ConversionEntry(5, "yd", "m", "yd ▶ m", v -> v * 0.9144));
        map.put(6, new ConversionEntry(6, "m", "yd", "m ▶ yd", v -> v / 0.9144));
        map.put(7, new ConversionEntry(7, "mile", "km", "mile ▶ km", v -> v * 1.609344));
        map.put(8, new ConversionEntry(8, "km", "mile", "km ▶ mile", v -> v / 1.609344));
        map.put(9, new ConversionEntry(9, "n mile", "m", "n mile ▶ m", v -> v * 1852.0));
        map.put(10, new ConversionEntry(10, "m", "n mile", "m ▶ n mile", v -> v / 1852.0));

        // Area & Volume
        map.put(11, new ConversionEntry(11, "acre", "m²", "acre ▶ m²", v -> v * 4046.8564224));
        map.put(12, new ConversionEntry(12, "m²", "acre", "m² ▶ acre", v -> v / 4046.8564224));
        map.put(13, new ConversionEntry(13, "gal (US)", "ℓ", "gal(US) ▶ ℓ", v -> v * 3.785411784));
        map.put(14, new ConversionEntry(14, "ℓ", "gal (US)", "ℓ ▶ gal(US)", v -> v / 3.785411784));
        map.put(15, new ConversionEntry(15, "gal (UK)", "ℓ", "gal(UK) ▶ ℓ", v -> v * 4.54609));
        map.put(16, new ConversionEntry(16, "ℓ", "gal (UK)", "ℓ ▶ gal(UK)", v -> v / 4.54609));
        map.put(17, new ConversionEntry(17, "pc", "km", "pc ▶ km", v -> v * 3.085677581e13));
        map.put(18, new ConversionEntry(18, "km", "pc", "km ▶ pc", v -> v / 3.085677581e13));

        // Velocity & Mass
        map.put(19, new ConversionEntry(19, "km/h", "m/s", "km/h ▶ m/s", v -> v / 3.6));
        map.put(20, new ConversionEntry(20, "m/s", "km/h", "m/s ▶ km/h", v -> v * 3.6));
        map.put(21, new ConversionEntry(21, "oz", "g", "oz ▶ g", v -> v * 28.349523125));
        map.put(22, new ConversionEntry(22, "g", "oz", "g ▶ oz", v -> v / 28.349523125));
        map.put(23, new ConversionEntry(23, "lb", "kg", "lb ▶ kg", v -> v * 0.45359237));
        map.put(24, new ConversionEntry(24, "kg", "lb", "kg ▶ lb", v -> v / 0.45359237));

        // Pressure
        map.put(25, new ConversionEntry(25, "atm", "Pa", "atm ▶ Pa", v -> v * 101325.0));
        map.put(26, new ConversionEntry(26, "Pa", "atm", "Pa ▶ atm", v -> v / 101325.0));
        map.put(27, new ConversionEntry(27, "mmHg", "Pa", "mmHg ▶ Pa", v -> v * 133.322));
        map.put(28, new ConversionEntry(28, "Pa", "mmHg", "Pa ▶ mmHg", v -> v / 133.322));

        // Power & Energy
        map.put(29, new ConversionEntry(29, "hp", "kW", "hp ▶ kW", v -> v * 0.74569987158227022));
        map.put(30, new ConversionEntry(30, "kW", "hp", "kW ▶ hp", v -> v / 0.74569987158227022));
        map.put(31, new ConversionEntry(31, "kgf/cm²", "Pa", "kgf/cm² ▶ Pa", v -> v * 98066.5));
        map.put(32, new ConversionEntry(32, "Pa", "kgf/cm²", "Pa ▶ kgf/cm²", v -> v / 98066.5));
        map.put(33, new ConversionEntry(33, "kgf·m", "J", "kgf·m ▶ J", v -> v * 9.80665));
        map.put(34, new ConversionEntry(34, "J", "kgf·m", "J ▶ kgf·m", v -> v / 9.80665));
        map.put(35, new ConversionEntry(35, "lbf/in²", "kPa", "lbf/in² ▶ kPa", v -> v * 6.894757293168));
        map.put(36, new ConversionEntry(36, "kPa", "lbf/in²", "kPa ▶ lbf/in²", v -> v / 6.894757293168));

        // Temperature & Energy
        map.put(37, new ConversionEntry(37, "°F", "°C", "°F ▶ °C", v -> (v - 32.0) * 5.0 / 9.0));
        map.put(38, new ConversionEntry(38, "°C", "°F", "°C ▶ °F", v -> v * 9.0 / 5.0 + 32.0));
        map.put(39, new ConversionEntry(39, "J", "cal", "J ▶ cal", v -> v / 4.184));
        map.put(40, new ConversionEntry(40, "cal", "J", "cal ▶ J", v -> v * 4.184));

        CONVERSIONS = Collections.unmodifiableMap(map);
    }

    public static ConversionEntry get(int number) {
        return CONVERSIONS.get(number);
    }

    public static double convert(int number, double value) {
        ConversionEntry entry = get(number);
        if (entry == null) throw new IllegalArgumentException("Unknown conversion code: " + number);
        return entry.converter().convert(value);
    }

    public static Map<Integer, ConversionEntry> getAll() {
        return CONVERSIONS;
    }
}
