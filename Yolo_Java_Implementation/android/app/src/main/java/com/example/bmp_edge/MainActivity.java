//package com.example.bmp_edge;
//
//import io.flutter.embedding.android.FlutterActivity;
//import io.flutter.embedding.engine.FlutterEngine;
//import io.flutter.plugin.common.MethodChannel;
//
//import java.util.ArrayList;
//
//import com.example.bmp_edge.yolo.YoloDetector;
//
//public class MainActivity extends FlutterActivity {
//
//    private static final String CHANNEL = "yolo";
//    private YoloDetector detector;
//
//    @Override
//    public void configureFlutterEngine(FlutterEngine flutterEngine) {
//
//        super.configureFlutterEngine(flutterEngine);
//
//        try {
//            detector = new YoloDetector(this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        new MethodChannel(
//                flutterEngine.getDartExecutor().getBinaryMessenger(),
//                CHANNEL
//        ).setMethodCallHandler((call, result) -> {
//
//            if (call.method.equals("detect")) {
//
//                ArrayList<Double> tensorList = call.argument("tensor");
//
//                float[] input = new float[tensorList.size()];
//
//                for (int i = 0; i < tensorList.size(); i++) {
//                    input[i] = tensorList.get(i).floatValue();
//                }
//
//                try {
//
//                    float[][][] output = detector.detect(input);
//
////                    result.success(output.toString());
////                    result.success(output[0].length);
//                    ArrayList<Float> flat = new ArrayList<>();
//
//                    for (int i = 0; i < output[0].length; i++) {
//                        for (int j = 0; j < output[0][i].length; j++) {
//                            flat.add(output[0][i][j]);
//                        }
//                    }
//
//                    result.success(flat);
//
//                } catch (Exception e) {
//
//                    result.error("ERROR", e.getMessage(), null);
//
//                }
//
//            }
//
//        });
//    }
//}
//
//



package com.example.bmp_edge;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import java.util.ArrayList;

import com.example.bmp_edge.yolo.YoloDetector;

public class MainActivity extends FlutterActivity {

    private static final String CHANNEL = "yolo";
    private YoloDetector detector;

    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {

        super.configureFlutterEngine(flutterEngine);

        try {
            detector = new YoloDetector(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new MethodChannel(
                flutterEngine.getDartExecutor().getBinaryMessenger(),
                CHANNEL
        ).setMethodCallHandler((call, result) -> {

            if (call.method.equals("detect")) {

                ArrayList<Double> tensorList = call.argument("tensor");

                float[] input = new float[tensorList.size()];

                for (int i = 0; i < tensorList.size(); i++) {
                    input[i] = tensorList.get(i).floatValue();
                }

                try {

                    float[][][] output = detector.detect(input);

                    ArrayList<Float> flat = new ArrayList<>();

                    for (int i = 0; i < output[0].length; i++) {
                        for (int j = 0; j < output[0][i].length; j++) {
                            flat.add(output[0][i][j]);
                        }
                    }

                    result.success(flat);

                } catch (Exception e) {
                    result.error("ERROR", e.getMessage(), null);
                }

            }

        });
    }
}
