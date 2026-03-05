class Detection {
  final String label;
  final double confidence;
  final double x, y, w, h;

  Detection({
    required this.label,
    required this.confidence,
    required this.x,
    required this.y,
    required this.w,
    required this.h,
  });
}
