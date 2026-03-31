import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_theme.dart';
import '../widgets/glowing_button.dart';

class LiveDetectionScreen extends StatefulWidget {
  const LiveDetectionScreen({super.key});

  @override
  State<LiveDetectionScreen> createState() => _LiveDetectionScreenState();
}

class _LiveDetectionScreenState extends State<LiveDetectionScreen>
    with TickerProviderStateMixin {
  static const _channel = MethodChannel('yolo_channel');

  bool _isDetecting = false;
  bool _isGpuEnabled = true;
  late AnimationController _scanlineController;
  late AnimationController _pulseController;
  late AnimationController _cornerController;
  late Animation<double> _scanlinePos;
  late Animation<double> _pulse;
  late Animation<double> _cornerScale;

  int _detectionCount = 0;
  String _inferenceTime = '--';

  @override
  void initState() {
    super.initState();
    _scanlineController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    _pulseController = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    )..repeat(reverse: true);
    _cornerController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    );
    _scanlinePos = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _scanlineController, curve: Curves.linear),
    );
    _pulse = Tween<double>(begin: 0.7, end: 1.0).animate(_pulseController);
    _cornerScale = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _cornerController, curve: Curves.elasticOut),
    );
    _cornerController.forward();
  }

  @override
  void dispose() {
    _scanlineController.dispose();
    _pulseController.dispose();
    _cornerController.dispose();
    super.dispose();
  }

  Future<void> _toggleDetection() async {
    HapticFeedback.mediumImpact();
    if (_isDetecting) {
      _scanlineController.stop();
      _scanlineController.reset();
      setState(() {
        _isDetecting = false;
        _inferenceTime = '--';
      });
    } else {
      setState(() => _isDetecting = true);
      _scanlineController.repeat();
      try {
        await _channel.invokeMethod('startDetection');
        setState(() => _detectionCount++);
      } catch (e) {
        // Demo mode
        setState(() {
          _inferenceTime = '${(12 + (8 * _pulse.value)).toStringAsFixed(1)}ms';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.bgDeep,
      body: SafeArea(
        child: Column(
          children: [
            // Header
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
              child: Row(
                children: [
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Live Detection',
                          style: TextStyle(
                            color: AppTheme.textPrimary,
                            fontSize: 24,
                            fontWeight: FontWeight.w800,
                            letterSpacing: -0.5,
                          ),
                        ),
                        Text(
                          'YOLOv8 · Real-time inference',
                          style: TextStyle(
                            color: AppTheme.textMuted,
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 20),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
              child: Column(
                children: [
                  const SizedBox(height: 16),
                  GlowingButton(
                    label: _isDetecting
                        ? 'Stop Detection'
                        : 'Start Live Detection',
                    onPressed: _toggleDetection,
                    gradientColors: _isDetecting
                        ? [AppTheme.accent, Color(0xFFCC1F80)]
                        : [AppTheme.primary, AppTheme.secondary],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoPill extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final Color color;

  const _InfoPill({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        decoration: BoxDecoration(
          color: AppTheme.bgCard,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppTheme.bgElevated),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: color, size: 16),
            const SizedBox(height: 4),
            Text(
              value,
              style: const TextStyle(
                color: AppTheme.textPrimary,
                fontWeight: FontWeight.w800,
                fontSize: 15,
              ),
            ),
            Text(
              label,
              style: const TextStyle(
                color: AppTheme.textMuted,
                fontSize: 10,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ScanGridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppTheme.primary.withOpacity(0.05)
      ..strokeWidth = 1;
    const spacing = 30.0;
    for (double x = 0; x < size.width; x += spacing) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += spacing) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(_) => false;
}

class _CornerPainter extends CustomPainter {
  final bool active;
  _CornerPainter({required this.active});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = active ? AppTheme.primary : AppTheme.textMuted.withOpacity(0.4)
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    const len = 28.0;
    final corners = [
      [Offset(0, len), Offset.zero, Offset(len, 0)],
      [
        Offset(size.width - len, 0),
        Offset(size.width, 0),
        Offset(size.width, len),
      ],
      [
        Offset(0, size.height - len),
        Offset(0, size.height),
        Offset(len, size.height),
      ],
      [
        Offset(size.width - len, size.height),
        Offset(size.width, size.height),
        Offset(size.width, size.height - len),
      ],
    ];

    for (final corner in corners) {
      final path = Path()
        ..moveTo(corner[0].dx, corner[0].dy)
        ..lineTo(corner[1].dx, corner[1].dy)
        ..lineTo(corner[2].dx, corner[2].dy);
      canvas.drawPath(path, paint);
    }
  }

  @override
  bool shouldRepaint(_CornerPainter old) => old.active != active;
}
