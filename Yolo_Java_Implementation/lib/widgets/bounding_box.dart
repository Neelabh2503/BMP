// import 'package:flutter/material.dart';
//
// import '../services/coco_labels.dart';
// import '../services/yolo_parser.dart';
//
// class BoundingBox extends StatelessWidget {
//   final List<Detection> detections;
//
//   BoundingBox(this.detections);
//
//   @override
//   Widget build(BuildContext context) {
//     return Stack(
//       children: detections.map((d) {
//         return Positioned(
//           left: d.x,
//           top: d.y,
//           width: d.w,
//           height: d.h,
//
//           child: Container(
//             decoration: BoxDecoration(
//               border: Border.all(color: Colors.red, width: 2),
//             ),
//
//             child: Text(
//               labels[d.classId],
//               style: TextStyle(
//                 backgroundColor: Colors.red,
//                 color: Colors.white,
//               ),
//             ),
//           ),
//         );
//       }).toList(),
//     );
//   }
// }
//

import 'package:flutter/material.dart';

import '../services/coco_labels.dart';
import '../services/yolo_parser.dart';

class BoundingBox extends StatelessWidget {
  final List<Detection> detections;
  final double screenW;
  final double screenH;

  const BoundingBox(this.detections, this.screenW, this.screenH, {super.key});

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: detections.map((d) {
        // YOLO uses 640x640 reference
        double scaleX = screenW / 640;
        double scaleY = screenH / 640;

        double left = (d.x - d.w / 2) * scaleX;
        double top = (d.y - d.h / 2) * scaleY;
        double width = d.w * scaleX;
        double height = d.h * scaleY;

        final label = d.classId < labels.length ? labels[d.classId] : "unknown";

        return Positioned(
          left: left,
          top: top,
          width: width,
          height: height,
          child: Container(
            decoration: BoxDecoration(
              border: Border.all(color: Colors.red, width: 3),
            ),
            child: Align(
              alignment: Alignment.topLeft,
              child: Container(
                color: Colors.red,
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                child: Text(
                  "$label ${(d.score * 100).toStringAsFixed(1)}%",
                  style: const TextStyle(color: Colors.white, fontSize: 12),
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}
