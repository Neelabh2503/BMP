import 'package:flutter/material.dart';

// class AppTheme {
//   static const Color primary = Color(0xFF4F8EF7); // Confident blue
//   static const Color secondary = Color(0xFF6C63FF); // Muted indigo-violet
//   static const Color accent = Color(0xFF00C896); // Calm emerald green
//   static const Color warning = Color(0xFFF0A500); // Warm amber
//
//   static const Color bgDeep = Color(0xFF060910);
//   static const Color bgCard = Color(0xFF0C1220);
//   static const Color bgSurface = Color(0xFF111828);
//   static const Color bgElevated = Color(0xFF192236);
//
//   static const Color glassLight = Color(0x14FFFFFF); // 8% white
//   static const Color glassBorder = Color(0x1FFFFFFF); // 12% white border
//
//   static const Color textPrimary = Color(0xFFE8EDF8);
//   static const Color textSecondary = Color(0xFF7A8BAD);
//   static const Color textMuted = Color(0xFF3D4F6E);
//
//   static const LinearGradient primaryGradient = LinearGradient(
//     colors: [primary, secondary],
//     begin: Alignment.topLeft,
//     end: Alignment.bottomRight,
//   );
//
//   static const LinearGradient cardGradient = LinearGradient(
//     colors: [Color(0x1A4F8EF7), Color(0x0A6C63FF)],
//     begin: Alignment.topLeft,
//     end: Alignment.bottomRight,
//   );
//
//   static const LinearGradient bgGradient = LinearGradient(
//     colors: [bgDeep, Color(0xFF080D18), bgDeep],
//     begin: Alignment.topCenter,
//     end: Alignment.bottomCenter,
//   );
//
//   static BoxDecoration glassCard({double borderRadius = 20, Color? tint}) =>
//       BoxDecoration(
//         borderRadius: BorderRadius.circular(borderRadius),
//         color: tint ?? glassLight,
//         border: Border.all(color: glassBorder, width: 1),
//         gradient: const LinearGradient(
//           colors: [Color(0x1AFFFFFF), Color(0x06FFFFFF)],
//           begin: Alignment.topLeft,
//           end: Alignment.bottomRight,
//         ),
//       );
//
//   static ThemeData get darkTheme => ThemeData(
//     useMaterial3: true,
//     brightness: Brightness.dark,
//     scaffoldBackgroundColor: bgDeep,
//     colorScheme: const ColorScheme.dark(
//       primary: primary,
//       secondary: secondary,
//       surface: bgCard,
//       onPrimary: Color(0xFFFFFFFF),
//       onSecondary: textPrimary,
//       onSurface: textPrimary,
//     ),
//     textTheme: const TextTheme(
//       displayLarge: TextStyle(
//         fontFamily: 'SF Pro Display',
//         fontSize: 34,
//         fontWeight: FontWeight.w700,
//         color: textPrimary,
//         letterSpacing: -0.8,
//       ),
//       titleLarge: TextStyle(
//         fontSize: 20,
//         fontWeight: FontWeight.w600,
//         color: textPrimary,
//         letterSpacing: -0.3,
//       ),
//       bodyLarge: TextStyle(
//         fontSize: 16,
//         color: textSecondary,
//         fontWeight: FontWeight.w400,
//         height: 1.5,
//       ),
//       bodyMedium: TextStyle(fontSize: 14, color: textSecondary, height: 1.4),
//     ),
//     inputDecorationTheme: InputDecorationTheme(
//       filled: true,
//       fillColor: bgSurface,
//       border: OutlineInputBorder(
//         borderRadius: BorderRadius.circular(14),
//         borderSide: const BorderSide(color: bgElevated),
//       ),
//       enabledBorder: OutlineInputBorder(
//         borderRadius: BorderRadius.circular(14),
//         borderSide: const BorderSide(color: bgElevated),
//       ),
//       focusedBorder: OutlineInputBorder(
//         borderRadius: BorderRadius.circular(14),
//         borderSide: const BorderSide(color: primary, width: 1.5),
//       ),
//       errorBorder: OutlineInputBorder(
//         borderRadius: BorderRadius.circular(14),
//         borderSide: const BorderSide(color: accent),
//       ),
//       labelStyle: const TextStyle(color: textSecondary),
//       prefixIconColor: textSecondary,
//     ),
//     elevatedButtonTheme: ElevatedButtonThemeData(
//       style: ElevatedButton.styleFrom(
//         backgroundColor: primary,
//         foregroundColor: Colors.white,
//         shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
//         padding: const EdgeInsets.symmetric(vertical: 16),
//         elevation: 0,
//         textStyle: const TextStyle(
//           fontWeight: FontWeight.w600,
//           fontSize: 16,
//           letterSpacing: 0.3,
//         ),
//       ),
//     ),
//     cardTheme: CardThemeData(
//       color: bgCard,
//       elevation: 0,
//       shape: RoundedRectangleBorder(
//         borderRadius: BorderRadius.circular(20),
//         side: const BorderSide(color: glassBorder, width: 1),
//       ),
//     ),
//     dividerColor: bgElevated,
//     iconTheme: const IconThemeData(color: textSecondary),
//   );
// }
//

class AppTheme {
  static const Color primary = Color(0xFF2563EB);
  static const Color secondary = Color(0xFF7C3AED);
  static const Color accent = Color(0xFF059669);
  static const Color warning = Color(0xFFD97706);
  static const Color bgDeep = Color(0xFFF0F4FF);
  static const Color bgCard = Color(0xFFFFFFFF);
  static const Color bgSurface = Color(0xFFF8FAFF);
  static const Color bgElevated = Color(0xFFEAEFF8);
  static const Color glassLight = Color(0xCCFFFFFF);
  static const Color glassBorder = Color(0x33C0CCEE);
  static const Color textPrimary = Color(0xFF0F172A);
  static const Color textSecondary = Color(0xFF475569);
  static const Color textMuted = Color(0xFF94A3B8);
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
        fontFamily: 'SF Pro Display',
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
      bodyLarge: TextStyle(
        fontSize: 16,
        color: textSecondary,
        fontWeight: FontWeight.w400,
        height: 1.5,
      ),
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
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: Color(0xFFDC2626)),
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
        textStyle: const TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 16,
          letterSpacing: 0.3,
        ),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: primary,
        side: const BorderSide(color: primary, width: 1.5),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        padding: const EdgeInsets.symmetric(vertical: 16),
        textStyle: const TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 16,
          letterSpacing: 0.3,
        ),
      ),
    ),
    cardTheme: CardThemeData(
      color: bgCard,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: const BorderSide(color: glassBorder, width: 1),
      ),
      shadowColor: const Color(0x142563EB),
    ),
    dividerColor: bgElevated,
    iconTheme: const IconThemeData(color: textSecondary),
  );
}
