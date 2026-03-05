import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';

import '../../../core/widgets/custom_components.dart';
import '../../vision/screens/object_detection_page.dart';
import 'signup_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage>
    with SingleTickerProviderStateMixin {
  late AnimationController _waveController;

  @override
  void initState() {
    super.initState();
    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat();
  }

  @override
  void dispose() {
    _waveController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

    return Scaffold(
      backgroundColor: const Color(0xFF0A0E21),
      body: Stack(
        children: [
          AnimatedBuilder(
            animation: _waveController,
            builder: (context, child) {
              return ClipPath(
                clipper: WaveClipper(_waveController.value),
                child: Container(
                  height: size.height * 0.45,
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.bottomLeft,
                      end: Alignment.topRight,
                      colors: [Color(0xFF00B4DB), Color(0xFF0083B0)],
                    ),
                  ),
                ),
              );
            },
          ),

          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 30),
              child: Column(
                children: [
                  const SizedBox(height: 20),

                  SizedBox(
                    height: 180,
                    child: Lottie.network(
                      'https://assets5.lottiefiles.com/packages/lf20_96bovdur.json',
                      fit: BoxFit.contain,
                    ),
                  ),

                  const Text(
                    "VISION",
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 34,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 3,
                    ),
                  ),
                  const Text(
                    "Edge CV ",
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 12,
                      letterSpacing: 4,
                    ),
                  ),

                  const SizedBox(height: 60),
                  _buildGlassPanel(
                    child: const Column(
                      children: [
                        CustomTextField(
                          label: "Email Address",
                          icon: Icons.email_outlined,
                        ),
                        SizedBox(height: 20),
                        CustomTextField(
                          label: "Password",
                          icon: Icons.lock_outline,
                          isPassword: true,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 40),

                  ActionButton(
                    text: "Sign In",
                    onPressed: () {
                      Navigator.pushReplacement(
                        context,
                        MaterialPageRoute(
                          builder: (_) => const ObjectDetectionPage(),
                        ),
                      );
                    },
                    gradientColors: const [
                      Color(0xFF00B4DB),
                      Color(0xFF0083B0),
                    ],
                  ),

                  const SizedBox(height: 30),

                  GestureDetector(
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const SignupPage(),
                      ),
                    ),
                    child: const Text(
                      "CREATE NEW ACCOUNT",
                      style: TextStyle(
                        color: Colors.cyanAccent,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 1.5,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGlassPanel({required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(30),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
      ),
      child: child,
    );
  }
}

class WaveClipper extends CustomClipper<Path> {
  final double animationValue;
  WaveClipper(this.animationValue);

  @override
  Path getClip(Size size) {
    Path path = Path();
    path.lineTo(0, size.height - 40);

    for (double i = 0; i <= size.width; i++) {
      path.lineTo(
        i,
        size.height -
            40 +
            math.sin(
                  (i / size.width * 2 * math.pi) +
                      (animationValue * 2 * math.pi),
                ) *
                20,
      );
    }

    path.lineTo(size.width, 0);
    path.close();
    return path;
  }

  @override
  bool shouldReclip(CustomClipper<Path> oldClipper) => true;
}
