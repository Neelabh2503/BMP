import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import 'screens/auth_screen.dart';
import 'screens/home_screen.dart';
import 'theme/app_theme.dart';
import 'theme/theme_notifier.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await Supabase.initialize(
    url: 'https://smfilbttxlutjatkvyjm.supabase.co',
    anonKey:
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNtZmlsYnR0eGx1dGphdGt2eWptIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ5NzY1MTAsImV4cCI6MjA5MDU1MjUxMH0.NaZtUdD0RFZ_-Fm3UKZEpgAFLzqUaHnrfk-TC854CRE',
  );

  final themeNotifier = ThemeNotifier();
  await themeNotifier.load();

  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
    ),
  );

  runApp(
    ChangeNotifierProvider.value(
      value: themeNotifier,
      child: const EdgeSiteApp(),
    ),
  );
}

class EdgeSiteApp extends StatelessWidget {
  const EdgeSiteApp({super.key});

  @override
  Widget build(BuildContext context) {
    final themeNotifier = context.watch<ThemeNotifier>();
    final session = Supabase.instance.client.auth.currentSession;

    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness:
            themeNotifier.isDark ? Brightness.light : Brightness.dark,
      ),
    );

    return MaterialApp(
      title: 'EdgeSite',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: themeNotifier.themeMode,
      home: session != null ? const HomeScreen() : const AuthScreen(),
    );
  }
}
