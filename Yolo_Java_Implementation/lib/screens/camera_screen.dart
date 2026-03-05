// import 'package:camera/camera.dart';
// import 'package:flutter/material.dart';
//
// import 'result_screen.dart';
//
// class CameraScreen extends StatefulWidget {
//   @override
//   State<CameraScreen> createState() => _CameraScreenState();
// }
//
// class _CameraScreenState extends State<CameraScreen> {
//   CameraController? controller;
//
//   @override
//   void initState() {
//     super.initState();
//     initCamera();
//   }
//
//   Future initCamera() async {
//     final cameras = await availableCameras();
//
//     controller = CameraController(
//       cameras[0],
//       ResolutionPreset.medium,
//       enableAudio: false,
//     );
//
//     await controller!.initialize();
//
//     setState(() {});
//   }
//
//   Future capture() async {
//     final image = await controller!.takePicture();
//
//     Navigator.pushReplacement(
//       context,
//       MaterialPageRoute(builder: (_) => ResultScreen(image.path)),
//     );
//   }
//
//   @override
//   Widget build(BuildContext context) {
//     if (controller == null || !controller!.value.isInitialized) {
//       return const Scaffold(body: Center(child: CircularProgressIndicator()));
//     }
//
//     return Scaffold(
//       appBar: AppBar(title: const Text("Capture Image")),
//       body: Stack(
//         children: [
//           CameraPreview(controller!),
//
//           Positioned(
//             bottom: 40,
//             left: 0,
//             right: 0,
//             child: Center(
//               child: FloatingActionButton(
//                 onPressed: capture,
//                 child: const Icon(Icons.camera),
//               ),
//             ),
//           ),
//         ],
//       ),
//     );
//   }
// }

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';

import 'result_screen.dart';

class CameraScreen extends StatefulWidget {
  const CameraScreen({Key? key}) : super(key: key);

  @override
  State<CameraScreen> createState() => _CameraScreenState();
}

class _CameraScreenState extends State<CameraScreen> {
  CameraController? controller;

  @override
  void initState() {
    super.initState();
    initCamera();
  }

  Future initCamera() async {
    final cameras = await availableCameras();

    controller = CameraController(
      cameras.first,
      ResolutionPreset.medium,
      enableAudio: false,
    );

    await controller!.initialize();

    if (!mounted) return;

    setState(() {});
  }

  Future capture() async {
    final image = await controller!.takePicture();

    if (!mounted) return;

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (_) => ResultScreen(image.path)),
    );
  }

  @override
  void dispose() {
    controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (controller == null || !controller!.value.isInitialized) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(title: const Text("Capture Image")),
      body: Stack(
        children: [
          CameraPreview(controller!),
          Positioned(
            bottom: 40,
            left: 0,
            right: 0,
            child: Center(
              child: FloatingActionButton(
                onPressed: capture,
                child: const Icon(Icons.camera),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
