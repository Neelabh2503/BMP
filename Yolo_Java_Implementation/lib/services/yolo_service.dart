// import 'package:flutter/services.dart';
//
// class YoloService {
//   static const MethodChannel _channel = MethodChannel("yolo");
//
//   static Future<List<dynamic>> detect(List<double> tensor) async {
//     final result = await _channel.invokeMethod("detect", {"tensor": tensor});
//
//     return result;
//   }
// }

import 'package:flutter/services.dart';

class YoloService {
  static const MethodChannel _channel = MethodChannel("yolo");

  static Future<List<dynamic>> detect(List<double> tensor) async {
    final result = await _channel.invokeMethod("detect", {"tensor": tensor});

    return result;
  }
}
