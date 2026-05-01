import 'package:flutter/material.dart';


class AppTheme {

  static const Color primary   = Color(0xFF2563EB);
  static const Color secondary = Color(0xFF7C3AED);
  static const Color accent    = Color(0xFF059669);
  static const Color warning   = Color(0xFFD97706);

  static const Color bgDeep     = Color(0xFFF0F4FF);
  static const Color bgCard     = Color(0xFFFFFFFF);
  static const Color bgSurface  = Color(0xFFF8FAFF);
  static const Color bgElevated = Color(0xFFEAEFF8);

  static const Color glassLight  = Color(0xCCFFFFFF);
  static const Color glassBorder = Color(0x33C0CCEE);

  static const Color textPrimary   = Color(0xFF0F172A);
  static const Color textSecondary = Color(0xFF475569);
  static const Color textMuted     = Color(0xFF94A3B8);

  static const Color darkPrimary   = Color(0xFF4B8EFF);
  static const Color darkSecondary = Color(0xFF9B6DFF);
  static const Color darkAccent    = Color(0xFF34D399);

  static const Color darkBgDeep     = Color(0xFF111827);
  static const Color darkBgCard     = Color(0xFF1E2535);
  static const Color darkBgSurface  = Color(0xFF1A2030);
  static const Color darkBgElevated = Color(0xFF252E42);

  static const Color darkGlassBorder = Color(0x28FFFFFF);

  static const Color darkTextPrimary   = Color(0xFFE2E8F0);
  static const Color darkTextSecondary = Color(0xFF94A3B8);
  static const Color darkTextMuted     = Color(0xFF475569);

  static const LinearGradient primaryGradient = LinearGradient(
    colors: [primary, secondary],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient cardGradient = LinearGradient(
    colors: [Color(0xFFFFFFFF), Color(0xFFF0F4FF)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient bgGradient = LinearGradient(
    colors: [Color(0xFFEEF2FF), Color(0xFFF8FAFF), Color(0xFFEEF2FF)],
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
  );

  static BoxDecoration neuCard({double radius = 16}) => BoxDecoration(
    color: bgCard,
    borderRadius: BorderRadius.circular(radius),
    boxShadow: const [
      BoxShadow(
        color: Color(0x18B0C4DE), 
        blurRadius: 12,
        offset: Offset(4, 4),
      ),
      BoxShadow(
        color: Color(0xEEFFFFFF), 
        blurRadius: 12,
        offset: Offset(-4, -4),
      ),
    ],
  );

  static BoxDecoration neuCardPressed({double radius = 16}) => BoxDecoration(
    color: bgCard,
    borderRadius: BorderRadius.circular(radius),
    boxShadow: const [
      BoxShadow(
        color: Color(0x22B0C4DE),
        blurRadius: 6,
        offset: Offset(2, 2),
      ),
      BoxShadow(
        color: Color(0xCCFFFFFF),
        blurRadius: 6,
        offset: Offset(-2, -2),
      ),
    ],
  );

  static BoxDecoration neuCardDark({double radius = 16}) => BoxDecoration(
    color: darkBgCard,
    borderRadius: BorderRadius.circular(radius),
    boxShadow: const [
      BoxShadow(
        color: Color(0x55000000),
        blurRadius: 12,
        offset: Offset(4, 4),
      ),
      BoxShadow(
        color: Color(0x22FFFFFF),
        blurRadius: 12,
        offset: Offset(-4, -4),
      ),
    ],
  );
  static BoxDecoration glassCard({double borderRadius = 20, Color? tint}) =>
      BoxDecoration(
        borderRadius: BorderRadius.circular(borderRadius),
        color: tint ?? glassLight,
        border: Border.all(color: glassBorder, width: 1),
        gradient: const LinearGradient(
          colors: [Color(0xF0FFFFFF), Color(0xD0F0F4FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: const [
          BoxShadow(
            color: Color(0x142563EB),
            blurRadius: 24,
            offset: Offset(0, 8),
          ),
        ],
      );

  static ThemeData get lightTheme => ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    scaffoldBackgroundColor: bgDeep,
    colorScheme: const ColorScheme.light(
      primary: primary,
      secondary: secondary,
      surface: bgCard,
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onSurface: textPrimary,
    ),
    textTheme: const TextTheme(
      displayLarge: TextStyle(
        fontSize: 34,
        fontWeight: FontWeight.w700,
        color: textPrimary,
        letterSpacing: -0.8,
      ),
      titleLarge: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        color: textPrimary,
        letterSpacing: -0.3,
      ),
      bodyLarge: TextStyle(fontSize: 16, color: textSecondary, height: 1.5),
      bodyMedium: TextStyle(fontSize: 14, color: textSecondary, height: 1.4),
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: bgCard,
      foregroundColor: textPrimary,
      elevation: 0,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        color: textPrimary,
        letterSpacing: -0.2,
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: bgSurface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: bgElevated),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: bgElevated),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: primary, width: 1.5),
      ),
      labelStyle: const TextStyle(color: textSecondary),
      prefixIconColor: textSecondary,
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: primary,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        padding: const EdgeInsets.symmetric(vertical: 16),
        elevation: 0,
        textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: primary,
        side: const BorderSide(color: primary, width: 1.5),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        padding: const EdgeInsets.symmetric(vertical: 16),
        textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
      ),
    ),
    cardTheme: CardThemeData(
      color: bgCard,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: const BorderSide(color: glassBorder, width: 1),
      ),
    ),
    dividerColor: bgElevated,
    iconTheme: const IconThemeData(color: textSecondary),
  );

  static ThemeData get darkTheme => ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: darkBgDeep,
    colorScheme: const ColorScheme.dark(
      primary: darkPrimary,
      secondary: darkSecondary,
      surface: darkBgCard,
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onSurface: darkTextPrimary,
    ),
    textTheme: const TextTheme(
      displayLarge: TextStyle(
        fontSize: 34,
        fontWeight: FontWeight.w700,
        color: darkTextPrimary,
        letterSpacing: -0.8,
      ),
      titleLarge: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        color: darkTextPrimary,
        letterSpacing: -0.3,
      ),
      bodyLarge: TextStyle(fontSize: 16, color: darkTextSecondary, height: 1.5),
      bodyMedium: TextStyle(fontSize: 14, color: darkTextSecondary, height: 1.4),
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: darkBgCard,
      foregroundColor: darkTextPrimary,
      elevation: 0,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        color: darkTextPrimary,
        letterSpacing: -0.2,
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: darkBgSurface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: darkBgElevated),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: darkBgElevated),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: darkPrimary, width: 1.5),
      ),
      labelStyle: const TextStyle(color: darkTextSecondary),
      prefixIconColor: darkTextSecondary,
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: darkPrimary,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        padding: const EdgeInsets.symmetric(vertical: 16),
        elevation: 0,
        textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: darkPrimary,
        side: const BorderSide(color: darkPrimary, width: 1.5),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        padding: const EdgeInsets.symmetric(vertical: 16),
        textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
      ),
    ),
    cardTheme: CardThemeData(
      color: darkBgCard,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: const BorderSide(color: darkGlassBorder, width: 1),
      ),
    ),
    dividerColor: darkBgElevated,
    iconTheme: const IconThemeData(color: darkTextSecondary),
  );
}
