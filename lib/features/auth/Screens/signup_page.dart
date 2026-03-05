import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';

import '../../../core/widgets/custom_components.dart';

class SignupPage extends StatefulWidget {
  const SignupPage({super.key});

  @override
  State<SignupPage> createState() => _SignupPageState();
}

class _SignupPageState extends State<SignupPage>
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
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: Stack(
        children: [
          AnimatedBuilder(
            animation: _waveController,
            builder: (context, child) {
              return ClipPath(
                clipper: SignupWaveClipper(_waveController.value),
                child: Container(
                  height: size.height * 0.42,
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
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
                  const SizedBox(height: 10),
                  SizedBox(
                    height: 160,
                    child: Lottie.network(
                      'https://lottie.host/83818318-971c-4348-89c0-9a2d2182741d/VjQx6t2Q8I.json',
                      errorBuilder: (context, error, stackTrace) {
                        return const Icon(
                          Icons.shield_outlined,
                          color: Colors.cyanAccent,
                          size: 80,
                        );
                      },
                    ),
                  ),

                  const Text(
                    "JOIN THE VISION",
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 32,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 1.5,
                    ),
                  ),
                  const Text(
                    "SECURE YOUR ACCOUNT ACCESS",
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 10,
                      letterSpacing: 2,
                    ),
                  ),

                  const SizedBox(height: 35),
                  _buildGlassPanel(
                    child: Column(
                      children: const [
                        CustomTextField(
                          label: "Full Name",
                          icon: Icons.person_outline,
                        ),
                        SizedBox(height: 18),
                        CustomTextField(
                          label: "Email Address",
                          icon: Icons.mail_outline,
                        ),
                        SizedBox(height: 18),
                        CustomTextField(
                          label: "Create Password",
                          icon: Icons.lock_open_outlined,
                          isPassword: true,
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 35),

                  ActionButton(
                    text: "GET STARTED",
                    onPressed: () {},
                    gradientColors: const [
                      Color(0xFF00B4DB),
                      Color(0xFF0083B0),
                    ],
                  ),

                  const SizedBox(height: 25),

                  GestureDetector(
                    onTap: () => Navigator.pop(context),
                    child: RichText(
                      text: const TextSpan(
                        text: "MEMBER ALREADY? ",
                        style: TextStyle(color: Colors.white60, fontSize: 13),
                        children: [
                          TextSpan(
                            text: "SIGN IN",
                            style: TextStyle(
                              color: Colors.cyanAccent,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 30),
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
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.04),
        borderRadius: BorderRadius.circular(25),
        border: Border.all(color: Colors.white.withOpacity(0.08)),
      ),
      child: child,
    );
  }
}

class SignupWaveClipper extends CustomClipper<Path> {
  final double animationValue;
  SignupWaveClipper(this.animationValue);

  @override
  Path getClip(Size size) {
    Path path = Path();
    path.lineTo(0, size.height - 50);
    for (double i = 0; i <= size.width; i++) {
      path.lineTo(
        i,
        size.height -
            50 +
            math.sin(
                  (i / size.width * 2 * math.pi) +
                      (animationValue * 2 * math.pi),
                ) *
                15,
      );
    }
    path.lineTo(size.width, 0);
    path.close();
    return path;
  }

  @override
  bool shouldReclip(CustomClipper<Path> oldClipper) => true;
}
