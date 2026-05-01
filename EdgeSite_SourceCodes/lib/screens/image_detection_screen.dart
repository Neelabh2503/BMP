import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../theme/app_theme.dart';
import '../theme/theme_notifier.dart';

class ImageDetectionScreen extends StatefulWidget {
  const ImageDetectionScreen({super.key});

  @override
  State<ImageDetectionScreen> createState() => _ImageDetectionScreenState();
}

class _ImageDetectionScreenState extends State<ImageDetectionScreen>
    with TickerProviderStateMixin {
  static const _channel = MethodChannel('yolo_channel');
  bool _isProcessing = false;
  bool _hasResult = false;

  late AnimationController _entryController;
  late AnimationController _processingController;
  late Animation<double> _entryFade;
  late Animation<double> _processingRotation;

  @override
  void initState() {
    super.initState();
    _entryController = AnimationController(
      duration: const Duration(milliseconds: 500),
      vsync: this,
    )..forward();
    _processingController = AnimationController(
      duration: const Duration(milliseconds: 900),
      vsync: this,
    );
    _entryFade = CurvedAnimation(
      parent: _entryController,
      curve: Curves.easeOut,
    );
    _processingRotation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _processingController, curve: Curves.linear),
    );
  }

  @override
  void dispose() {
    _entryController.dispose();
    _processingController.dispose();
    super.dispose();
  }

  Future<void> _detectImage() async {
    HapticFeedback.mediumImpact();
    setState(() {
      _isProcessing = true;
      _hasResult = false;
    });
    _processingController.repeat();

    try {
      await _channel.invokeMethod('detectImage');
    } catch (_) {}

    await Future.delayed(const Duration(seconds: 2));
    _processingController.stop();
    _processingController.reset();
    setState(() {
      _isProcessing = false;
      _hasResult = true;
    });
  }

  @override
  Widget build(BuildContext context) {
    final isDark      = context.watch<ThemeNotifier>().isDark;
    final bg          = isDark ? AppTheme.darkBgDeep : AppTheme.bgDeep;
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;
    final textMuted   = isDark ? AppTheme.darkTextSecondary : AppTheme.textMuted;
    final primary     = isDark ? AppTheme.darkPrimary : AppTheme.primary;

    return Scaffold(
      backgroundColor: bg,
      body: SafeArea(
        child: FadeTransition(
          opacity: _entryFade,
          child: SingleChildScrollView(
            physics: const BouncingScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Image Scan',
                  style: TextStyle(
                    color: textPrimary,
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'YOLOv8 · Photo inference',
                  style: TextStyle(color: textMuted, fontSize: 13),
                ),

                const SizedBox(height: 24),

                _DropZone(
                  isDark: isDark,
                  isProcessing: _isProcessing,
                  hasResult: _hasResult,
                  primaryColor: primary,
                  processingRotation: _processingRotation,
                ),

                const SizedBox(height: 20),

                _NeuButton(
                  label: _isProcessing ? 'Scanning…' : 'Pick Image & Detect',
                  icon: _isProcessing
                      ? Icons.hourglass_top_rounded
                      : Icons.image_search_rounded,
                  accentColor: primary,
                  isDark: isDark,
                  enabled: !_isProcessing,
                  onTap: _isProcessing ? null : _detectImage,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DropZone extends StatelessWidget {
  final bool isDark;
  final bool isProcessing;
  final bool hasResult;
  final Color primaryColor;
  final Animation<double> processingRotation;

  const _DropZone({
    required this.isDark,
    required this.isProcessing,
    required this.hasResult,
    required this.primaryColor,
    required this.processingRotation,
  });

  @override
  Widget build(BuildContext context) {
    final cardBg = isDark ? AppTheme.darkBgCard : AppTheme.bgCard;

    return Container(
      width: double.infinity,
      height: 200,
      decoration: BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: primaryColor.withOpacity(isProcessing ? 0.5 : 0.15),
          width: isProcessing ? 1.5 : 1,
        ),
        boxShadow: isDark
            ? [
                const BoxShadow(color: Color(0x44000000), blurRadius: 12, offset: Offset(4, 4)),
                const BoxShadow(color: Color(0x14FFFFFF), blurRadius: 12, offset: Offset(-4, -4)),
              ]
            : [
                const BoxShadow(color: Color(0x18B0C4DE), blurRadius: 12, offset: Offset(4, 4)),
                const BoxShadow(color: Color(0xCCFFFFFF), blurRadius: 12, offset: Offset(-4, -4)),
              ],
      ),
      child: Center(
        child: isProcessing
            ? Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  RotationTransition(
                    turns: processingRotation,
                    child: Icon(Icons.radar_rounded,
                        color: primaryColor, size: 48),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Analyzing…',
                    style: TextStyle(
                      color: primaryColor,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              )
            : hasResult
                ? Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.check_circle_rounded,
                          color: AppTheme.accent, size: 48),
                      const SizedBox(height: 8),
                      Text(
                        'Detection complete',
                        style: TextStyle(
                          color: isDark
                              ? AppTheme.darkTextPrimary
                              : AppTheme.textPrimary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  )
                : Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.add_photo_alternate_rounded,
                        color: (isDark
                                ? AppTheme.darkTextSecondary
                                : AppTheme.textMuted)
                            .withOpacity(0.5),
                        size: 48,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Tap below to pick an image',
                        style: TextStyle(
                          color: isDark
                              ? AppTheme.darkTextSecondary
                              : AppTheme.textMuted,
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
      ),
    );
  }
}

class _NeuButton extends StatefulWidget {
  final String label;
  final IconData icon;
  final Color accentColor;
  final bool isDark;
  final bool enabled;
  final VoidCallback? onTap;

  const _NeuButton({
    required this.label,
    required this.icon,
    required this.accentColor,
    required this.isDark,
    required this.enabled,
    required this.onTap,
  });

  @override
  State<_NeuButton> createState() => _NeuButtonState();
}

class _NeuButtonState extends State<_NeuButton>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;

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
      onTapDown: widget.enabled ? (_) => _ctrl.reverse() : null,
      onTapUp: widget.enabled
          ? (_) {
              _ctrl.forward();
              widget.onTap?.call();
            }
          : null,
      onTapCancel: () => _ctrl.forward(),
      child: ScaleTransition(
        scale: _ctrl,
        child: Opacity(
          opacity: widget.enabled ? 1.0 : 0.5,
          child: Container(
            width: double.infinity,
            padding:
                const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
            decoration: BoxDecoration(
              color: bgCard,
              borderRadius: BorderRadius.circular(16),
              boxShadow: widget.isDark
                  ? const [
                      BoxShadow(
                          color: Color(0x44000000),
                          blurRadius: 10,
                          offset: Offset(4, 4)),
                      BoxShadow(
                          color: Color(0x1AFFFFFF),
                          blurRadius: 10,
                          offset: Offset(-4, -4)),
                    ]
                  : const [
                      BoxShadow(
                          color: Color(0x18B0C4DE),
                          blurRadius: 10,
                          offset: Offset(4, 4)),
                      BoxShadow(
                          color: Color(0xCCFFFFFF),
                          blurRadius: 10,
                          offset: Offset(-4, -4)),
                    ],
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(widget.icon, color: widget.accentColor, size: 22),
                const SizedBox(width: 12),
                Text(
                  widget.label,
                  style: TextStyle(
                    color: widget.isDark
                        ? AppTheme.darkTextPrimary
                        : AppTheme.textPrimary,
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
