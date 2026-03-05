import 'package:flutter/material.dart';

import 'camera_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("YOLO Object Detection")),
      body: Center(
        child: ElevatedButton.icon(
          icon: const Icon(Icons.camera_alt),
          label: const Text("Capture Image"),
          style: ElevatedButton.styleFrom(
            padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15),
            textStyle: const TextStyle(fontSize: 18),
          ),
          onPressed: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => CameraScreen()),
            );
          },
        ),
      ),
    );
  }
}
