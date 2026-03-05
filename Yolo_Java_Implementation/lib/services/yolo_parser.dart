// class Detection {
//   final double x;
//   final double y;
//   final double w;
//   final double h;
//   final int classId;
//   final double score;
//
//   Detection(this.x, this.y, this.w, this.h, this.classId, this.score);
// }
//
// List<Detection> parseYOLO(List<double> output) {
//   const int numAnchors = 8400;
//   const int numClasses = 80;
//   const int stride = 84;
//
//   List<Detection> detections = [];
//
//   for (int i = 0; i < numAnchors; i++) {
//     double x = output[i];
//     double y = output[8400 + i];
//     double w = output[16800 + i];
//     double h = output[25200 + i];
//
//     double maxScore = 0;
//     int classId = -1;
//
//     for (int c = 0; c < numClasses; c++) {
//       double score = output[(4 + c) * 8400 + i];
//
//       if (score > maxScore) {
//         maxScore = score;
//         classId = c;
//       }
//     }
//
//     if (maxScore > 0.4) {
//       detections.add(Detection(x, y, w, h, classId, maxScore));
//     }
//   }
//
//   return detections;
// }

class Detection {
  final double x;
  final double y;
  final double w;
  final double h;
  final int classId;
  final double score;

  Detection(this.x, this.y, this.w, this.h, this.classId, this.score);
}

List<Detection> parseYOLO(List<double> output) {
  const int numAnchors = 8400;
  const int numClasses = 80;

  List<Detection> detections = [];

  for (int i = 0; i < numAnchors; i++) {
    double x = output[i];
    double y = output[8400 + i];
    double w = output[16800 + i];
    double h = output[25200 + i];

    double maxScore = 0;
    int classId = -1;

    for (int c = 0; c < numClasses; c++) {
      double score = output[(4 + c) * 8400 + i];

      if (score > maxScore) {
        maxScore = score;
        classId = c;
      }
    }

    if (maxScore > 0.45 && classId >= 0) {
      detections.add(Detection(x, y, w, h, classId, maxScore));
    }
  }

  return detections;
}
