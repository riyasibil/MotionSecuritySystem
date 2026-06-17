import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class FishTrackingSystem {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    // ---------------- INTERFACE ----------------
    interface Tracker {
        void processFrame(Mat frame);
    }

    // ---------------- BASE CLASS (INHERITANCE) ----------------
    static class BaseProcessor {
        public Mat preprocess(Mat frame) {
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            return gray;
        }
    }

    // ---------------- FISH TRACKER (IMPLEMENTS + EXTENDS) ----------------
    static class FishTracker extends BaseProcessor implements Tracker {

        @Override
        public void processFrame(Mat frame) {

            Mat gray = preprocess(frame);

            Mat thresh = new Mat();
            Imgproc.threshold(gray, thresh, 100, 255, Imgproc.THRESH_BINARY_INV);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();

            Imgproc.findContours(thresh, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE);

            if (!contours.isEmpty()) {

                double maxArea = 0;
                int maxIndex = 0;

                for (int i = 0; i < contours.size(); i++) {
                    double area = Imgproc.contourArea(contours.get(i));
                    if (area > maxArea) {
                        maxArea = area;
                        maxIndex = i;
                    }
                }

                Moments m = Imgproc.moments(contours.get(maxIndex));

                if (m.get_m00() != 0) {
                    int cx = (int) (m.get_m10() / m.get_m00());
                    int cy = (int) (m.get_m01() / m.get_m00());

                    System.out.println("Fish detected at (x,y): (" + cx + "," + cy + ")");

                    // Generate password
                    String password = "" + cx + cy;
                    System.out.println("Generated Password: " + password);

                    // Draw point
                    Imgproc.circle(frame, new Point(cx, cy), 10, new Scalar(0, 0, 255), -1);
                }
            }

            gray.release();
            thresh.release();
            hierarchy.release();
        }
    }

    // ---------------- CAMERA MANAGER ----------------
    static class CameraManager {

        private VideoCapture camera;

        public boolean startCamera() {
            camera = new VideoCapture(0);
            return camera.isOpened();
        }

        public Mat getFrame() {
            Mat frame = new Mat();
            camera.read(frame);
            return frame;
        }

        public void stopCamera() {
            if (camera != null) camera.release();
        }
    }

    // ---------------- MAIN METHOD ----------------
    public static void main(String[] args) {

        CameraManager camera = new CameraManager();
        FishTracker tracker = new FishTracker();

        if (!camera.startCamera()) {
            System.out.println("❌ Camera not detected");
            return;
        }

        System.out.println("✅ Camera started");

        long lastCaptureTime = 0;
        int captureInterval = 30 * 1000; // 30 sec

        while (true) {

            Mat frame = camera.getFrame();
            if (frame.empty()) break;

            HighGui.imshow("Live Feed", frame);

            long currentTime = System.currentTimeMillis();

            // EVENT-BASED TRIGGER (every 30 seconds)
            if (currentTime - lastCaptureTime >= captureInterval) {
                tracker.processFrame(frame);
                lastCaptureTime = currentTime;
            }

            // Exit on ESC
            if (HighGui.waitKey(1) == 27) break;
        }

        camera.stopCamera();
        HighGui.destroyAllWindows();

        System.out.println("✅ Program ended");
    }
}
