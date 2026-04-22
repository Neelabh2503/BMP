import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import 'screens/auth_screen.dart';
import 'screens/home_screen.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Supabase.initialize(
    url: 'https://smfilbttxlutjatkvyjm.supabase.co',
    anonKey:
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNtZmlsYnR0eGx1dGphdGt2eWptIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ5NzY1MTAsImV4cCI6MjA5MDU1MjUxMH0.NaZtUdD0RFZ_-Fm3UKZEpgAFLzqUaHnrfk-TC854CRE',
  );
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
    ),
  );
  runApp(const EdgeSiteApp());
}

class EdgeSiteApp extends StatelessWidget {
  const EdgeSiteApp({super.key});

  @override
  Widget build(BuildContext context) {
    final session = Supabase.instance.client.auth.currentSession;
    return MaterialApp(
      title: 'EdgeSite',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      home: session != null ? const HomeScreen() : const AuthScreen(),
    );
  }
}
