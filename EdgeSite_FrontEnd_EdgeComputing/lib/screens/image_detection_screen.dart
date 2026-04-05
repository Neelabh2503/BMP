import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_theme.dart';
import '../widgets/glowing_button.dart';

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
  late AnimationController _resultController;
  late Animation<double> _entryFade;
  late Animation<double> _processingRotation;
  late Animation<double> _resultScale;

  final List<Map<String, dynamic>> _mockResults = [
    {'label': 'Person', 'conf': 0.96, 'color': AppTheme.primary},
    {'label': 'Laptop', 'conf': 0.88, 'color': AppTheme.secondary},
    {'label': 'Cup', 'conf': 0.72, 'color': AppTheme.warning},
  ];

  @override
  void initState() {
    super.initState();
    _entryController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    )..forward();
    _processingController = AnimationController(
      duration: const Duration(milliseconds: 1000),
      vsync: this,
    );
    _resultController = AnimationController(
      duration: const Duration(milliseconds: 500),
      vsync: this,
    );

    _entryFade = CurvedAnimation(
      parent: _entryController,
      curve: Curves.easeOut,
    );
    _processingRotation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _processingController, curve: Curves.linear),
    );
    _resultScale = CurvedAnimation(
      parent: _resultController,
      curve: Curves.elasticOut,
    );
  }

  @override
  void dispose() {
    _entryController.dispose();
    _processingController.dispose();
    _resultController.dispose();
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
    _resultController.forward(from: 0);
    HapticFeedback.heavyImpact();
  }

  void _reset() {
    setState(() {
      _hasResult = false;
      _isProcessing = false;
    });
    _resultController.reset();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.bgDeep,
      body: SafeArea(
        child: FadeTransition(
          opacity: _entryFade,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                child: Row(
                  children: [
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Image Scan',
                            style: TextStyle(
                              color: AppTheme.textPrimary,
                              fontSize: 24,
                              fontWeight: FontWeight.w800,
                              letterSpacing: -0.5,
                            ),
                          ),
                          Text(
                            'Pick an image to analyze',
                            style: TextStyle(
                              color: AppTheme.textMuted,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (_hasResult)
                      GestureDetector(
                        onTap: _reset,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 8,
                          ),
                          decoration: BoxDecoration(
                            color: AppTheme.bgCard,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: AppTheme.bgElevated),
                          ),
                          child: const Text(
                            'Reset',
                            style: TextStyle(
                              color: AppTheme.textSecondary,
                              fontWeight: FontWeight.w600,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),

              const SizedBox(height: 20),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
                child: Column(
                  children: [
                    if (!_hasResult) ...[
                      Row(
                        children: [
                          Expanded(
                            child: _SourceButton(
                              icon: Icons.photo_library_rounded,
                              label: 'Gallery',
                              color: AppTheme.secondary,
                              onTap: _detectImage,
                            ),
                          ),
                        ],
                      ),
                    ] else ...[
                      GlowingButton(
                        label: 'Analyze Another Image',
                        onPressed: () {
                          _reset();
                          Future.delayed(
                            const Duration(milliseconds: 200),
                            _detectImage,
                          );
                        },
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDropZone() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              color: AppTheme.primary.withOpacity(0.08),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.add_photo_alternate_outlined,
              color: AppTheme.primary,
              size: 36,
            ),
          ),
          const SizedBox(height: 18),
          const Text(
            'Drop an image here',
            style: TextStyle(
              color: AppTheme.textPrimary,
              fontWeight: FontWeight.w700,
              fontSize: 17,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'or use the buttons below',
            style: TextStyle(color: AppTheme.textMuted, fontSize: 13),
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: AppTheme.bgSurface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppTheme.bgElevated),
            ),
            child: const Text(
              'JPG · PNG · WEBP',
              style: TextStyle(
                color: AppTheme.textMuted,
                fontSize: 11,
                fontWeight: FontWeight.w600,
                letterSpacing: 1,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MockBoundingBox extends StatelessWidget {
  final String label;
  final Color color;
  final double width;
  final double height;

  const _MockBoundingBox({
    required this.label,
    required this.color,
    required this.width,
    required this.height,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        border: Border.all(color: color, width: 2),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Align(
        alignment: Alignment.topLeft,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
          color: color,
          child: Text(
            label,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 8,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ),
    );
  }
}

class _SourceButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _SourceButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 56,
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: color, size: 22),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: color,
                fontWeight: FontWeight.w700,
                fontSize: 15,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
