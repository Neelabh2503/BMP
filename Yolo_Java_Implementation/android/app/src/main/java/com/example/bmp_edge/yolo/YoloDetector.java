package com.example.bmp_edge.yolo;

import android.content.Context;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;

import ai.onnxruntime.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class YoloDetector {

    OrtEnvironment env;
    OrtSession session;

    public YoloDetector(Context context) throws Exception {

        env = OrtEnvironment.getEnvironment();

        InputStream is = context.getAssets().open("yolov8n.onnx");

        byte[] model = new byte[is.available()];
        is.read(model);

        session = env.createSession(model);
    }


    public float[][][] detectImage(String imagePath) throws Exception {

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        Bitmap resized = Bitmap.createScaledBitmap(bitmap,640,640,true);

        float[] input = new float[3*640*640];

        int rIndex = 0;
        int gIndex = 640*640;
        int bIndex = 2*640*640;

        for(int y=0;y<640;y++){
            for(int x=0;x<640;x++){

                int pixel = resized.getPixel(x,y);

                input[rIndex++] = ((pixel>>16)&0xFF)/255f;
                input[gIndex++] = ((pixel>>8)&0xFF)/255f;
                input[bIndex++] = (pixel&0xFF)/255f;

            }
        }

        OnnxTensor tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(input),
                new long[]{1,3,640,640}
        );

        OrtSession.Result result = session.run(
                Collections.singletonMap("images",tensor)
        );

        return (float[][][]) result.get(0).getValue();

    }
    public float[][][] detect(float[] inputTensor) throws Exception {

        OnnxTensor tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(inputTensor),
                new long[]{1,3,640,640}
        );

        OrtSession.Result result = session.run(
                Collections.singletonMap("images", tensor)
        );

        return (float[][][]) result.get(0).getValue();
    }
}