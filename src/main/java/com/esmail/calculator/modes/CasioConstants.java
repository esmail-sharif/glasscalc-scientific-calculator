package com.esmail.calculator.modes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The 40 scientific constants of CASIO fx-991ES PLUS / fx-570ES PLUS (Shift + 7 [CONST]).
 * Values are based on CODATA recommended values.
 */
public final class CasioConstants {
    public record ConstantEntry(int number, String symbol, String name, double value, String unit) {}

    private static final Map<Integer, ConstantEntry> CONSTANTS_BY_NUM;
    private static final Map<String, ConstantEntry> CONSTANTS_BY_SYMBOL;

    static {
        Map<Integer, ConstantEntry> byNum = new LinkedHashMap<>();
        Map<String, ConstantEntry> bySym = new LinkedHashMap<>();

        add(byNum, bySym, 1, "mp", "Proton mass", 1.672621637e-27, "kg");
        add(byNum, bySym, 2, "mn", "Neutron mass", 1.674927211e-27, "kg");
        add(byNum, bySym, 3, "me", "Electron mass", 9.10938215e-31, "kg");
        add(byNum, bySym, 4, "mμ", "Muon mass", 1.88353130e-28, "kg");
        add(byNum, bySym, 5, "a0", "Bohr radius", 5.2917720859e-11, "m");
        add(byNum, bySym, 6, "h", "Planck constant", 6.62606896e-34, "J s");
        add(byNum, bySym, 7, "μN", "Nuclear magneton", 5.05078324e-27, "J/T");
        add(byNum, bySym, 8, "μB", "Bohr magneton", 9.27400915e-24, "J/T");
        add(byNum, bySym, 9, "ħ", "Rationalized Planck constant", 1.054571628e-34, "J s");
        add(byNum, bySym, 10, "α", "Fine-structure constant", 7.2973525376e-3, "");
        add(byNum, bySym, 11, "re", "Classical electron radius", 2.8179402894e-15, "m");
        add(byNum, bySym, 12, "λc", "Compton wavelength", 2.426310217e-12, "m");
        add(byNum, bySym, 13, "γp", "Proton gyromagnetic ratio", 2.675222099e8, "s^-1 T^-1");
        add(byNum, bySym, 14, "λcp", "Proton Compton wavelength", 1.3214098446e-15, "m");
        add(byNum, bySym, 15, "λcn", "Neutron Compton wavelength", 1.3159403848e-15, "m");
        add(byNum, bySym, 16, "R∞", "Rydberg constant", 10973731.568527, "m^-1");
        add(byNum, bySym, 17, "u", "Atomic mass unit", 1.660538782e-27, "kg");
        add(byNum, bySym, 18, "μp", "Proton magnetic moment", 1.410606662e-26, "J/T");
        add(byNum, bySym, 19, "μe", "Electron magnetic moment", -9.28476377e-24, "J/T");
        add(byNum, bySym, 20, "μn", "Neutron magnetic moment", -9.6623641e-27, "J/T");
        add(byNum, bySym, 21, "μμ", "Muon magnetic moment", -4.49044786e-26, "J/T");
        add(byNum, bySym, 22, "F", "Faraday constant", 96485.3399, "C/mol");
        add(byNum, bySym, 23, "e_c", "Elementary charge", 1.602176487e-19, "C");
        add(byNum, bySym, 24, "NA", "Avogadro constant", 6.02214179e23, "mol^-1");
        add(byNum, bySym, 25, "k", "Boltzmann constant", 1.3806504e-23, "J/K");
        add(byNum, bySym, 26, "Vm", "Molar volume of ideal gas", 0.022413996, "m^3/mol");
        add(byNum, bySym, 27, "R_gas", "Molar gas constant", 8.314472, "J/(mol K)");
        add(byNum, bySym, 28, "c0", "Speed of light in vacuum", 299792458.0, "m/s");
        add(byNum, bySym, 29, "C1", "First radiation constant", 3.74177118e-16, "W m^2");
        add(byNum, bySym, 30, "C2", "Second radiation constant", 0.014387752, "m K");
        add(byNum, bySym, 31, "σ", "Stefan-Boltzmann constant", 5.670400e-8, "W/(m^2 K^4)");
        add(byNum, bySym, 32, "ε0", "Electric constant (vacuum permittivity)", 8.854187817e-12, "F/m");
        add(byNum, bySym, 33, "μ0", "Magnetic constant (vacuum permeability)", 1.2566370614e-6, "N/A^2");
        add(byNum, bySym, 34, "Φ0", "Magnetic flux quantum", 2.067833667e-15, "Wb");
        add(byNum, bySym, 35, "g", "Standard gravity acceleration", 9.80665, "m/s^2");
        add(byNum, bySym, 36, "G0", "Conductance quantum", 7.7480917004e-5, "S");
        add(byNum, bySym, 37, "Z0", "Characteristic impedance of vacuum", 376.730313461, "Ω");
        add(byNum, bySym, 38, "t_c", "Celsius temperature offset", 273.15, "K");
        add(byNum, bySym, 39, "G", "Newtonian constant of gravitation", 6.67428e-11, "m^3/(kg s^2)");
        add(byNum, bySym, 40, "atm", "Standard atmospheric pressure", 101325.0, "Pa");

        CONSTANTS_BY_NUM = Collections.unmodifiableMap(byNum);
        CONSTANTS_BY_SYMBOL = Collections.unmodifiableMap(bySym);
    }

    private static void add(Map<Integer, ConstantEntry> byNum, Map<String, ConstantEntry> bySym,
                            int num, String symbol, String name, double value, String unit) {
        ConstantEntry entry = new ConstantEntry(num, symbol, name, value, unit);
        byNum.put(num, entry);
        bySym.put(symbol.toLowerCase(), entry);
    }

    public static ConstantEntry get(int number) {
        return CONSTANTS_BY_NUM.get(number);
    }

    public static ConstantEntry getBySymbol(String symbol) {
        if (symbol == null) return null;
        return CONSTANTS_BY_SYMBOL.get(symbol.toLowerCase());
    }

    public static Map<Integer, ConstantEntry> getAll() {
        return CONSTANTS_BY_NUM;
    }
}
