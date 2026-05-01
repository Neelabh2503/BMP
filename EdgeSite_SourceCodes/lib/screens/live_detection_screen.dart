import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../theme/app_theme.dart';
import '../theme/theme_notifier.dart';

class LiveDetectionScreen extends StatefulWidget {
  const LiveDetectionScreen({super.key});

  @override
  State<LiveDetectionScreen> createState() => _LiveDetectionScreenState();
}

class _LiveDetectionScreenState extends State<LiveDetectionScreen>
    with TickerProviderStateMixin {
  static const _channel = MethodChannel('yolo_channel');

  bool _isDetecting = false;
  late AnimationController _pulseController;
  late Animation<double> _pulse;
  late AnimationController _cornerController;
  late Animation<double> _cornerScale;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    )..repeat(reverse: true);
    _cornerController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    );
    _pulse = Tween<double>(begin: 0.7, end: 1.0).animate(_pulseController);
    _cornerScale = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _cornerController, curve: Curves.elasticOut),
    );
    _cornerController.forward();
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _cornerController.dispose();
    super.dispose();
  }

  Future<void> _startESPDetection() async {
    HapticFeedback.mediumImpact();
    try {
      await _channel.invokeMethod('startESPDetection');
    } catch (e) {
      debugPrint('ESP detection error: $e');
    }
  }

  Future<void> _startCloudCamera() async {
    HapticFeedback.mediumImpact();
    try {
      await _channel.invokeMethod('startCloudCamera');
    } catch (e) {
      debugPrint('Cloud camera error: $e');
    }
  }

  Future<void> _toggleDetection() async {
    HapticFeedback.mediumImpact();
    setState(() => _isDetecting = !_isDetecting);
    try {
      await _channel.invokeMethod('startDetection');
    } catch (e) {
      debugPrint('Detection error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = context.watch<ThemeNotifier>().isDark;
    final bg          = isDark ? AppTheme.darkBgDeep    : AppTheme.bgDeep;
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;
    final textMuted   = isDark ? AppTheme.darkTextSecondary : AppTheme.textMuted;
    final primary     = isDark ? AppTheme.darkPrimary   : AppTheme.primary;

    return Scaffold(
      backgroundColor: bg,
      body: SafeArea(
        child: SingleChildScrollView(
          physics: const BouncingScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Live Detection',
                style: TextStyle(
                  color: textPrimary,
                  fontSize: 24,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                'YOLOv8 · Real-time inference',
                style: TextStyle(color: textMuted, fontSize: 13),
              ),

              const SizedBox(height: 24),

              _NeuActionButton(
                label: _isDetecting ? 'Stop Detection' : 'Start Live Detection',
                icon: _isDetecting ? Icons.stop_rounded : Icons.videocam_rounded,
                accentColor: _isDetecting
                    ? const Color(0xFFDC2626)
                    : primary,
                isDark: isDark,
                onTap: _toggleDetection,
              ),

              const SizedBox(height: 12),

              _NeuActionButton(
                label: 'Connect to ESP32 CAM',
                icon: Icons.wifi_tethering_rounded,
                accentColor: const Color(0xFFF97316),
                isDark: isDark,
                onTap: _startESPDetection,
              ),

              const SizedBox(height: 12),

              _NeuActionButton(
                label: 'Connect to Channel',
                icon: Icons.cloud_sync_rounded,
                accentColor: isDark ? AppTheme.darkSecondary : AppTheme.secondary,
                isDark: isDark,
                onTap: _startCloudCamera,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NeuActionButton extends StatefulWidget {
  final String label;
  final IconData icon;
  final Color accentColor;
  final bool isDark;
  final VoidCallback onTap;

  const _NeuActionButton({
    required this.label,
    required this.icon,
    required this.accentColor,
    required this.isDark,
    required this.onTap,
  });

  @override
  State<_NeuActionButton> createState() => _NeuActionButtonState();
}

class _NeuActionButtonState extends State<_NeuActionButton>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 90),
      lowerBound: 0.96,
      upperBound: 1.0,
      value: 1.0,
    );
    _scale = _ctrl;
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bgCard = widget.isDark ? AppTheme.darkBgCard : AppTheme.bgCard;

    return GestureDetector(
      onTapDown: (_) => _ctrl.reverse(),
      onTapUp: (_) {
        _ctrl.forward();
        widget.onTap();
      },
      onTapCancel: () => _ctrl.forward(),
      child: ScaleTransition(
        scale: _scale,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
          decoration: BoxDecoration(
            color: bgCard,
            borderRadius: BorderRadius.circular(16),
            boxShadow: widget.isDark
                ? const [
                    BoxShadow(color: Color(0x44000000), blurRadius: 10, offset: Offset(4, 4)),
                    BoxShadow(color: Color(0x1AFFFFFF), blurRadius: 10, offset: Offset(-4, -4)),
                  ]
                : const [
                    BoxShadow(color: Color(0x18B0C4DE), blurRadius: 10, offset: Offset(4, 4)),
                    BoxShadow(color: Color(0xCCFFFFFF), blurRadius: 10, offset: Offset(-4, -4)),
                  ],
          ),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: widget.accentColor.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(widget.icon, color: widget.accentColor, size: 22),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  widget.label,
                  style: TextStyle(
                    color: widget.isDark
                        ? AppTheme.darkTextPrimary
                        : AppTheme.textPrimary,
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                color: widget.isDark
                    ? AppTheme.darkTextMuted
                    : AppTheme.textMuted,
                size: 20,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
