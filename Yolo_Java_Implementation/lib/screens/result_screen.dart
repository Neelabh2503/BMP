// import 'dart:io';
//
// import 'package:flutter/material.dart';
// import 'package:image/image.dart' as img;
//
// import '../services/yolo_parser.dart';
// import '../services/yolo_service.dart';
// import '../widgets/bounding_box.dart';
//
// class ResultScreen extends StatefulWidget {
//   final String imagePath;
//
//   const ResultScreen(this.imagePath, {super.key});
//
//   @override
//   State<ResultScreen> createState() => _ResultScreenState();
// }
//
// class _ResultScreenState extends State<ResultScreen> {
//   List<Detection> detections = [];
//   bool loading = true;
//
//   @override
//   void initState() {
//     super.initState();
//     runDetection();
//   }
//
//   Future runDetection() async {
//     final bytes = await File(widget.imagePath).readAsBytes();
//
//     img.Image? original = img.decodeImage(bytes);
//
//     img.Image resized = img.copyResize(original!, width: 640, height: 640);
//
//     List<double> tensor = [];
//
//     List<double> r = [];
//     List<double> g = [];
//     List<double> b = [];
//
//     for (int y = 0; y < 640; y++) {
//       for (int x = 0; x < 640; x++) {
//         final pixel = resized.getPixel(x, y);
//
//         r.add(pixel.r / 255.0);
//         g.add(pixel.g / 255.0);
//         b.add(pixel.b / 255.0);
//       }
//     }
//
//     tensor.addAll(r);
//     tensor.addAll(g);
//     tensor.addAll(b);
//
//     final result = await YoloService.detect(tensor);
//
//     final parsed = parseYOLO(List<double>.from(result));
//
//     setState(() {
//       detections = parsed;
//       loading = false;
//     });
//   }
//
//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(title: const Text("Detection Result")),
//
//       body: Stack(
//         children: [
//           Center(child: Image.file(File(widget.imagePath))),
//
//           BoundingBox(detections),
//
//           // BoundingBox(detections, imageWidth, imageHeight),
//           if (loading) const Center(child: CircularProgressIndicator()),
//         ],
//       ),
//     );
//   }
// }

// import 'dart:io';
//
// import 'package:flutter/foundation.dart';
// import 'package:flutter/material.dart';
// import 'package:image/image.dart' as img;
//
// import '../services/coco_labels.dart';
// import '../services/yolo_parser.dart';
// import '../services/yolo_service.dart';
// import '../widgets/bounding_box.dart';
//
// class ResultScreen extends StatefulWidget {
//   final String imagePath;
//
//   const ResultScreen(this.imagePath, {Key? key}) : super(key: key);
//
//   @override
//   State<ResultScreen> createState() => _ResultScreenState();
// }
//
// class _ResultScreenState extends State<ResultScreen> {
//   List<Detection> detections = [];
//   bool loading = true;
//
//   double imageWidth = 0;
//   double imageHeight = 0;
//
//   @override
//   void initState() {
//     super.initState();
//     runDetection();
//   }
//
//   Future runDetection() async {
//     final bytes = await File(widget.imagePath).readAsBytes();
//
//     final decoded = img.decodeImage(bytes)!;
//
//     imageWidth = decoded.width.toDouble();
//     imageHeight = decoded.height.toDouble();
//
//     final tensor = await compute(buildTensor, Uint8List.fromList(bytes));
//
//     final result = await YoloService.detect(tensor);
//
//     // final parsed = parseYOLO(List<double>.from(result));
//     // final parsed = parseYOLO(List<double>.from(result));
//     // print("detections: ${parsed.length}");
//
//     final parsed = parseYOLO(List<double>.from(result));
//
//     print("detections: ${parsed.length}");
//
//     for (var d in parsed) {
//       final name = labels[d.classId];
//       print("Detected: $name  confidence: ${d.score}");
//     }
//
//     setState(() {
//       detections = parsed;
//       loading = false;
//     });
//   }
//
//   static List<double> buildTensor(Uint8List bytes) {
//     final original = img.decodeImage(bytes)!;
//
//     final resized = img.copyResize(original, width: 640, height: 640);
//
//     List<double> tensor = List.filled(3 * 640 * 640, 0);
//
//     int rIndex = 0;
//     int gIndex = 640 * 640;
//     int bIndex = 2 * 640 * 640;
//
//     for (int y = 0; y < 640; y++) {
//       for (int x = 0; x < 640; x++) {
//         final pixel = resized.getPixel(x, y);
//
//         tensor[rIndex++] = pixel.r / 255.0;
//         tensor[gIndex++] = pixel.g / 255.0;
//         tensor[bIndex++] = pixel.b / 255.0;
//       }
//     }
//
//     return tensor;
//   }
//
//   // @override
//   // Widget build(BuildContext context) {
//   //   return Scaffold(
//   //     appBar: AppBar(title: const Text("Detection Result")),
//   //     body: LayoutBuilder(
//   //       builder: (context, constraints) {
//   //         return Stack(
//   //           children: [
//   //             Center(
//   //               child: Image.file(File(widget.imagePath), fit: BoxFit.contain),
//   //             ),
//   //             BoundingBox(
//   //               detections,
//   //               constraints.maxWidth,
//   //               constraints.maxHeight,
//   //             ),
//   //             if (loading) const Center(child: CircularProgressIndicator()),
//   //           ],
//   //         );
//   //       },
//   //     ),
//   //   );
//   // }
//
//   @override
//   Widget build(BuildContext context) {
//     final image = Image.file(File(widget.imagePath));
//
//     return Scaffold(
//       appBar: AppBar(title: const Text("Detection Result")),
//
//       body: LayoutBuilder(
//         builder: (context, constraints) {
//           double screenWidth = constraints.maxWidth;
//           double screenHeight = constraints.maxHeight;
//
//           return Stack(
//             children: [
//               Center(child: image),
//
//               BoundingBox(detections, screenWidth, screenHeight),
//
//               if (loading) const Center(child: CircularProgressIndicator()),
//             ],
//           );
//         },
//       ),
//     );
//   }
// }

import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:image/image.dart' as img;

import '../services/coco_labels.dart';
import '../services/yolo_parser.dart';
import '../services/yolo_service.dart';
import '../widgets/bounding_box.dart';

class ResultScreen extends StatefulWidget {
  final String imagePath;

  const ResultScreen(this.imagePath, {Key? key}) : super(key: key);

  @override
  State<ResultScreen> createState() => _ResultScreenState();
}

class _ResultScreenState extends State<ResultScreen> {
  List<Detection> detections = [];
  bool loading = true;

  double imageWidth = 0;
  double imageHeight = 0;

  @override
  void initState() {
    super.initState();
    runDetection();
  }

  Future runDetection() async {
    final bytes = await File(widget.imagePath).readAsBytes();

    final decoded = img.decodeImage(bytes)!;

    imageWidth = decoded.width.toDouble();
    imageHeight = decoded.height.toDouble();

    /// Build tensor in background isolate
    final tensor = await compute(buildTensor, Uint8List.fromList(bytes));

    /// Run ONNX inference
    final result = await YoloService.detect(tensor);

    final parsed = parseYOLO(List<double>.from(result));

    print("detections: ${parsed.length}");

    for (var d in parsed) {
      final name = labels[d.classId];
      print("Detected: $name confidence: ${d.score}");
    }

    setState(() {
      detections = parsed;
      loading = false;
    });
  }

  /// Builds tensor for YOLO input
  static List<double> buildTensor(Uint8List bytes) {
    final original = img.decodeImage(bytes)!;

    final resized = img.copyResize(original, width: 640, height: 640);

    List<double> tensor = List.filled(3 * 640 * 640, 0);

    int rIndex = 0;
    int gIndex = 640 * 640;
    int bIndex = 2 * 640 * 640;

    for (int y = 0; y < 640; y++) {
      for (int x = 0; x < 640; x++) {
        final pixel = resized.getPixel(x, y);

        tensor[rIndex++] = pixel.r / 255.0;
        tensor[gIndex++] = pixel.g / 255.0;
        tensor[bIndex++] = pixel.b / 255.0;
      }
    }

    return tensor;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Detection Result")),

      body: LayoutBuilder(
        builder: (context, constraints) {
          final screenW = constraints.maxWidth;
          final screenH = constraints.maxHeight;

          return Stack(
            children: [
              /// IMAGE
              Center(
                child: Image.file(
                  File(widget.imagePath),
                  fit: BoxFit.contain,
                  width: screenW,
                  height: screenH,
                ),
              ),

              /// BOUNDING BOXES
              BoundingBox(detections, screenW, screenH),

              /// LOADING
              if (loading) const Center(child: CircularProgressIndicator()),

              /// DETECTED OBJECT PANEL
              if (!loading)
                Align(
                  alignment: Alignment.bottomCenter,
                  child: Container(
                    width: double.infinity,
                    color: Colors.black.withOpacity(0.8),
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: detections.map((d) {
                        final label = d.classId < labels.length
                            ? labels[d.classId]
                            : "unknown";

                        return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 2),
                          child: Text(
                            "$label  ${(d.score * 100).toStringAsFixed(1)}%",
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 16,
                            ),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}
