package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    List<Path> undoPath = new ArrayList<>();
    List<Path> redoPath = new ArrayList<>();

    List<String> undoType = new ArrayList<>();
    List<String> redoType = new ArrayList<>();

    List<String> undoAction = new ArrayList<>();
    List<String> redoAction = new ArrayList<>();

    Map<Integer, List<Path>> savedUndoPath = new HashMap<>();
    Map<Integer, List<Path>> savedRedoPath = new HashMap<>();
    Map<Integer, List<String>> savedUndoType = new HashMap<>();
    Map<Integer, List<String>> savedRedoType = new HashMap<>();
    Map<Integer, List<String>> savedUndoAction = new HashMap<>();
    Map<Integer, List<String>> savedRedoAction = new HashMap<>();
    

    // drawing path
    Path path = null;
    boolean moved = false;
    List<Path> paths = new ArrayList();
    List<String> type = new ArrayList<>();

    Map<Integer, List<Path>> savedPath = new HashMap<>();
    Map<Integer, List<String>> savedType = new HashMap<>();

    // image to display
    Bitmap bitmap;
    Paint paint = new Paint(Color.BLUE);

    // constructor
    public PDFimage(Context context) {
        super(context);
        savedPath.put(0, new ArrayList<Path>());
        savedPath.put(1, new ArrayList<Path>());
        savedPath.put(2, new ArrayList<Path>());

        savedType.put(0, new ArrayList<String>());
        savedType.put(1, new ArrayList<String>());
        savedType.put(2, new ArrayList<String>());

        savedUndoPath.put(0, new ArrayList<Path>());
        savedUndoPath.put(1, new ArrayList<Path>());
        savedUndoPath.put(2, new ArrayList<Path>());

        savedRedoPath.put(0, new ArrayList<Path>());
        savedRedoPath.put(1, new ArrayList<Path>());
        savedRedoPath.put(2, new ArrayList<Path>());

        savedUndoAction.put(0, new ArrayList<String>());
        savedUndoAction.put(1, new ArrayList<String>());
        savedUndoAction.put(2, new ArrayList<String>());

        savedRedoAction.put(0, new ArrayList<String>());
        savedRedoAction.put(1, new ArrayList<String>());
        savedRedoAction.put(2, new ArrayList<String>());

        savedUndoType.put(0, new ArrayList<String>());
        savedUndoType.put(1, new ArrayList<String>());
        savedUndoType.put(2, new ArrayList<String>());

        savedRedoType.put(0, new ArrayList<String>());
        savedRedoType.put(1, new ArrayList<String>());
        savedRedoType.put(2, new ArrayList<String>());


        paths = savedPath.get(0);
        type = savedType.get(0);
        undoAction = savedUndoAction.get(0);
        undoType = savedUndoType.get(0);
        undoPath = savedUndoPath.get(0);

        redoAction = savedRedoAction.get(0);
        redoType = savedRedoType.get(0);
        redoPath = savedRedoPath.get(0);
    }

    float x1, x2, y1, y2, old_x1, old_y1, old_x2, old_y2;
    float mid_x = -1f, mid_y = -1f, old_mid_x = -1f, old_mid_y = -1f;
    int p1_id, p1_index, p2_id, p2_index;

    Matrix matrix = new Matrix();
    Matrix inverse = new Matrix();

    public void switchPages(int newPage) {
        paths = savedPath.get(newPage);
        type = savedType.get(newPage);
        undoPath = savedUndoPath.get(newPage);
        undoAction = savedUndoAction.get(newPage);
        undoType = savedUndoType.get(newPage);
        redoPath = savedRedoPath.get(newPage);
        redoType = savedRedoType.get(newPage);
        redoAction = savedRedoAction.get(newPage);
        matrix = new Matrix();
        inverse = new Matrix();
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float[] inverted;
        if (event.getPointerCount() == 1) {
            p1_id = event.getPointerId(0);
            p1_index = event.findPointerIndex(p1_id);

            // invert using the current matrix to account for pan/scale
            // inverts in-place and returns boolean
            inverse = new Matrix();
            matrix.invert(inverse);

            // mapPoints returns values in-place
            inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
            inverse.mapPoints(inverted);
            x1 = inverted[0];
            y1 = inverted[1];

            if (MainActivity.eraserActivated) {

                for (int i = 0; i < paths.size(); i++) {
                    RectF r = new RectF();
                    Point point = new Point((int) (x1), (int) (y1));

                    Path curPath = paths.get(i);
                    curPath.computeBounds(r, true);
                    if (r.contains(point.x, point.y)) {
                        undoPath.add(paths.get(i));
                        undoType.add(type.get(i));
                        undoAction.add("e");
                        redoPath.clear();
                        redoAction.clear();
                        redoType.clear();
                        paths.remove(i);
                        type.remove(i);
                        break;
                    }
                }
            }

            if (MainActivity.pencilActivated || MainActivity.highlighterActivated) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        path = new Path();
                        moved = false;
                        path.moveTo(x1,y1);
                        break;
                    case MotionEvent.ACTION_MOVE:

                        moved = true;
                        path.lineTo(x1, y1);
                        break;
                    case MotionEvent.ACTION_UP:

                        if (moved) {
                            paths.add(path);
                            undoPath.add(path);
                            undoAction.add("d");
                            if (MainActivity.pencilActivated) {
                                type.add("p");
                                undoType.add("p");
                            } else if (MainActivity.highlighterActivated) {
                                type.add("h");
                                undoType.add("h");
                            }
                            redoPath.clear();
                            redoAction.clear();
                            redoType.clear();
                        }
                         path = null;
                        break;
                }
            }
        } else if (event.getPointerCount() == 2) {
            // point 1
            p1_id = event.getPointerId(0);
            p1_index = event.findPointerIndex(p1_id);

            // mapPoints returns values in-place
            inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
            inverse.mapPoints(inverted);

            // first pass, initialize the old == current value
            if (old_x1 < 0 || old_y1 < 0) {
                old_x1 = x1 = inverted[0];
                old_y1 = y1 = inverted[1];
            } else {
                old_x1 = x1;
                old_y1 = y1;
                x1 = inverted[0];
                y1 = inverted[1];
            }

            // point 2
            p2_id = event.getPointerId(1);
            p2_index = event.findPointerIndex(p2_id);

            // mapPoints returns values in-place
            inverted = new float[] { event.getX(p2_index), event.getY(p2_index) };
            inverse.mapPoints(inverted);

            // first pass, initialize the old == current value
            if (old_x2 < 0 || old_y2 < 0) {
                old_x2 = x2 = inverted[0];
                old_y2 = y2 = inverted[1];
            } else {
                old_x2 = x2;
                old_y2 = y2;
                x2 = inverted[0];
                y2 = inverted[1];
            }

            // midpoint
            mid_x = (x1 + x2) / 2;
            mid_y = (y1 + y2) / 2;
            old_mid_x = (old_x1 + old_x2) / 2;
            old_mid_y = (old_y1 + old_y2) / 2;

            // distance
            float d_old = (float) Math.sqrt(Math.pow((old_x1 - old_x2), 2) + Math.pow((old_y1 - old_y2), 2));
            float d = (float) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

            // pan and zoom during MOVE event
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                // pan == translate of midpoint
                float dx = mid_x - old_mid_x;
                float dy = mid_y - old_mid_y;
                matrix.preTranslate(dx, dy);

                // zoom == change of spread between p1 and p2
                float scale = d/d_old;
                scale = Math.max(0, scale);
                matrix.preScale(scale, scale, mid_x, mid_y);

                // reset on up
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                old_x1 = -1f;
                old_y1 = -1f;
                old_x2 = -1f;
                old_y2 = -1f;
                old_mid_x = -1f;
                old_mid_y = -1f;
            }

        }

        invalidate();
        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush(Paint paint) {
        this.paint = paint;
    }

    public void undo() {

        if (undoPath.size() > 0) {

            int size = undoPath.size();
            Path p = undoPath.get(size - 1);
            String t = undoType.get(size - 1);
            String a = undoAction.get(size - 1);
            // System.out.println("action: " + a);
            undoPath.remove(size - 1);
            undoType.remove(size - 1);
            undoAction.remove(size - 1);
            redoPath.add(p);
            redoType.add(t);
            redoAction.add(a);

            if (a.equals("e")) {
                paths.add(p);
                type.add(t);
                invalidate();
            } else if (a.equals("d")) {
                int pIndex = paths.indexOf(p);
                paths.remove(p);
                type.remove(pIndex);
                invalidate();
            }
        }

    }

    public void redo() {
        int size = redoAction.size();
        if (size > 0) {
            Path p = redoPath.get(size - 1);
            String t = redoType.get(size - 1);
            String a = redoAction.get(size - 1);
            redoPath.remove(size - 1);
            redoType.remove(size - 1);
            redoAction.remove(size - 1);
            undoPath.add(p);
            undoType.add(t);
            undoAction.add(a);
            if (a.equals("d")) {
                paths.add(p);
                type.add(t);
                invalidate();
            } else if (a.equals("e")) {
                int pIndex = paths.indexOf(p);
                paths.remove(p);
                type.remove(pIndex);
                invalidate();
            }
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {

        // apply transformations from the event handler above
        canvas.concat(matrix);


        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }

        // draw lines over it
        for (int i = 0; i < paths.size(); i++) {
            Path curPath = paths.get(i);
            if (type.get(i).equals("p")) {
                paint.setColor(Color.BLUE);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
            } else if (type.get(i).equals("h")) {
                paint.setColor(Color.YELLOW);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(15);
            }
            canvas.drawPath(curPath, paint);
        }

        if (path != null) {
            if (MainActivity.pencilActivated) {
                paint.setColor(Color.BLUE);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
            } else if (MainActivity.highlighterActivated) {
                paint.setColor(Color.YELLOW);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(15);
            }
            canvas.drawPath(path, paint);
        }

        super.onDraw(canvas);

    }
}
