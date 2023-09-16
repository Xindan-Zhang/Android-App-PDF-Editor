package net.codebot.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "example.pdf";
    final int FILERESID = R.raw.example;

    ImageButton pencil;
    ImageButton eraser;
    ImageButton highlighter;

    Button undo;
    Button redo;
    public static boolean pencilActivated = false;
    public static boolean eraserActivated = false;
    public static boolean highlighterActivated = false;

    Button previous;
    Button next;
    int pageNumber = 0;

    TextView pageNumberView;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;
    private static final int TOTAL_PAGE = 3;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        pageImage = new PDFimage(this);
        layout.addView(pageImage, 2);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        TextView title = findViewById(R.id.title);

        pencil = findViewById(R.id.pen);
        pencil.setBackgroundColor(Color.LTGRAY);
        pencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //System.out.println("clicked pencil");
                pencilActivated = !pencilActivated;
                if (pencilActivated) {
                    pencil.setBackgroundColor(Color.RED);
                    eraserActivated = false;
                    highlighterActivated = false;
                    eraser.setBackgroundColor(Color.LTGRAY);
                    highlighter.setBackgroundColor(Color.LTGRAY);
                } else {
                    pencil.setBackgroundColor(Color.LTGRAY);
                }
            }
        });

        eraser = findViewById(R.id.eraser);
        eraser.setBackgroundColor(Color.LTGRAY);
        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //System.out.println("clicked pencil");
                eraserActivated = !eraserActivated;
                if (eraserActivated) {
                    eraser.setBackgroundColor(Color.RED);
                    pencilActivated = false;
                    pencil.setBackgroundColor(Color.LTGRAY);
                    highlighterActivated = false;
                    highlighter.setBackgroundColor(Color.LTGRAY);
                } else {
                    eraser.setBackgroundColor(Color.LTGRAY);
                }
            }
        });

        highlighter = findViewById(R.id.highlighter);
        highlighter.setBackgroundColor(Color.LTGRAY);
        highlighter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //System.out.println("clicked pencil");
                highlighterActivated = !highlighterActivated;
                if (highlighterActivated) {
                    highlighter.setBackgroundColor(Color.RED);
                    pencilActivated = false;
                    eraserActivated = false;
                    pencil.setBackgroundColor(Color.LTGRAY);
                    eraser.setBackgroundColor(Color.LTGRAY);
                } else {
                    highlighter.setBackgroundColor(Color.LTGRAY);
                }
            }
        });

        undo = findViewById(R.id.undo);
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageImage.undo();

            }
        });

        redo = findViewById(R.id.redo);
        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageImage.redo();

            }
        });

        previous = findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pageNumber > 0) {
                    pageNumber--;
                    switchPage();

                }

            }
        });

        next = findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pageNumber < TOTAL_PAGE - 1) {
                    pageNumber++;
                    switchPage();
                }

            }
        });

        pageNumberView = findViewById(R.id.pageNumber);

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage
        try {
            openRenderer(this);
            showPage(pageNumber);
            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void switchPage() {
        try {
            pageImage.switchPages(pageNumber);
            openRenderer(this);
            showPage(pageNumber);
            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }

        String pageNumberText = "Page " + (index + 1) + "/" + TOTAL_PAGE;

        pageNumberView.setText(pageNumberText);

        currentPage = pdfRenderer.openPage(index);

        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);
    }
}
