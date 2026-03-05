import 'dart:async';
import 'dart:typed_data';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:image/image.dart' as img;
import 'package:tflite_flutter/tflite_flutter.dart';

import '../../../main.dart';
import '../data/labels.dart';
import '../models/detection.dart';

class ObjectDetectionPage extends StatefulWidget {
  const ObjectDetectionPage({super.key});

  @override
  State<ObjectDetectionPage> createState() => _ObjectDetectionPageState();
}

class _ObjectDetectionPageState extends State<ObjectDetectionPage> {
  late CameraController _camera;
  late Interpreter _interpreter;

  bool _isDetecting = false;
  bool _modelLoaded = false;

  List<Detection> _detections = [];

  static const int inputSize = 640;
  static const double threshold = 0.4;

  @override
  void initState() {
    super.initState();

    _initAll();
  }

  Future<void> _initAll() async {
    await _loadModel();
    await _initCamera();
  }

  // ---------------- MODEL ----------------

  Future<void> _loadModel() async {
    _interpreter = await Interpreter.fromAsset(
      'assets/models/yolo.tflite',
      options: InterpreterOptions()..threads = 4,
    );

    _modelLoaded = true;
  }

  // ---------------- CAMERA ----------------

  Future<void> _initCamera() async {
    _camera = CameraController(
      cameras.first,
      ResolutionPreset.medium,
      enableAudio: false,
    );

    await _camera.initialize();

    await _camera.startImageStream(_processFrame);

    if (mounted) setState(() {});
  }

  // ---------------- FRAME PROCESSING ----------------

  Future<void> _processFrame(CameraImage image) async {
    if (!_modelLoaded || _isDetecting) return;

    _isDetecting = true;

    final input = _preprocess(image);

    final output = List.generate(
      1,
      (_) => List.generate(84, (_) => List.filled(8400, 0.0)),
    );

    _interpreter.run(input.reshape([1, 3, inputSize, inputSize]), output);

    final detections = _parseOutput(output[0]);

    if (mounted) {
      setState(() {
        _detections = detections;
      });
    }

    _isDetecting = false;
  }

  // ---------------- PREPROCESS ----------------

  Float32List _preprocess(CameraImage image) {
    final bytes = _yuvToRgb(image);

    img.Image src = img.Image.fromBytes(
      width: image.width,
      height: image.height,
      bytes: bytes.buffer,
      format: img.Format.uint8,
    );

    img.Image resized = img.copyResize(
      src,
      width: inputSize,
      height: inputSize,
    );

    final Float32List input = Float32List(1 * 3 * inputSize * inputSize);

    int i = 0;

    for (int y = 0; y < inputSize; y++) {
      for (int x = 0; x < inputSize; x++) {
        final p = resized.getPixel(x, y);

        input[i++] = p.r / 255.0;
        input[i++] = p.g / 255.0;
        input[i++] = p.b / 255.0;
      }
    }

    return input; // ✅ Return Float32List directly
  }

  Uint8List _yuvToRgb(CameraImage image) {
    final int width = image.width;
    final int height = image.height;

    final y = image.planes[0].bytes;
    final u = image.planes[1].bytes;
    final v = image.planes[2].bytes;

    final Uint8List out = Uint8List(width * height * 3);

    int i = 0;

    for (int h = 0; h < height; h++) {
      for (int w = 0; w < width; w++) {
        final int uvIndex = (h ~/ 2) * image.planes[1].bytesPerRow + (w ~/ 2);

        final int yp = y[h * image.planes[0].bytesPerRow + w];
        final int up = u[uvIndex];
        final int vp = v[uvIndex];

        int r = (yp + 1.370705 * (vp - 128)).round();
        int g = (yp - 0.337633 * (up - 128) - 0.698001 * (vp - 128)).round();
        int b = (yp + 1.732446 * (up - 128)).round();

        out[i++] = r.clamp(0, 255);
        out[i++] = g.clamp(0, 255);
        out[i++] = b.clamp(0, 255);
      }
    }

    return out;
  }

  // ---------------- POSTPROCESS ----------------

  List<Detection> _parseOutput(List<List<double>> output) {
    List<Detection> detections = [];

    for (int i = 0; i < 8400; i++) {
      double conf = output[4][i];

      if (conf < threshold) continue;

      int cls = 0;
      double max = 0;

      for (int j = 5; j < 85; j++) {
        if (output[j][i] > max) {
          max = output[j][i];
          cls = j - 5;
        }
      }

      final x = output[0][i];
      final y = output[1][i];
      final w = output[2][i];
      final h = output[3][i];

      detections.add(
        Detection(
          label: yoloLabels[cls],
          confidence: conf,
          x: x,
          y: y,
          w: w,
          h: h,
        ),
      );
    }

    return detections;
  }

  // ---------------- UI ----------------

  @override
  void dispose() {
    _camera.dispose();
    _interpreter.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_camera.value.isInitialized) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(title: const Text("YOLOv8 Detection")),
      body: Stack(
        children: [
          CameraPreview(_camera),

          CustomPaint(painter: _BoxPainter(_detections)),
        ],
      ),
    );
  }
}

// ---------------- BOX PAINTER ----------------

class _BoxPainter extends CustomPainter {
  final List<Detection> detections;

  _BoxPainter(this.detections);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.green
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke;

    final textStyle = const TextStyle(
      color: Colors.green,
      fontSize: 14,
      fontWeight: FontWeight.bold,
    );

    for (var d in detections) {
      final rect = Rect.fromLTWH(
        d.x * size.width,
        d.y * size.height,
        d.w * size.width,
        d.h * size.height,
      );

      canvas.drawRect(rect, paint);

      final tp = TextPainter(
        text: TextSpan(
          text: "${d.label} ${(d.confidence * 100).toInt()}%",
          style: textStyle,
        ),
        textDirection: TextDirection.ltr,
      );

      tp.layout();

      tp.paint(canvas, Offset(rect.left, rect.top - 18));
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
