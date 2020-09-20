package app.ij.hackricexretinopathyapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;

import android.os.ProxyFileDescriptorCallback;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ResultsActivity extends AppCompatActivity {

    TextView title;
    Button share;
    Bitmap image;
    ImageView imageView;
    Uri imageUri;
    TextView result;
    Interpreter tflite;
    boolean retino = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Get the Uri from previous Activity
        Intent receiveIntent = getIntent();
        imageView = findViewById(R.id.image_view);
        imageUri = receiveIntent.getParcelableExtra("image");
        setImage();

        Button startBtn = (Button) findViewById(R.id.sendEmail);
        share = findViewById(R.id.share);
        title = findViewById(R.id.title);
        title.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        startBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sendEmail();
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareWithEveryone();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        result = findViewById(R.id.results);

        loadFile();
    }

    private void classifyImage(Bitmap bitmap) {
        int imageSizeX = 224;
        int imageSizeY = 224;

        // initialize output array
        float[][] inputVal = new float[1][1];
        bitmap = getResizedBitmap(bitmap, imageSizeX, imageSizeY);

        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 1 *
                224 * 224 * 3); //float_size = 4 bytes
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }

        // pass bitmap into the graph
        tflite.run(byteBuffer, inputVal);
        Log.wtf("*Prediction", "Result: " + inputVal[0][0]);
        if (inputVal[0][0] >= 0.5) {
            if (inputVal[0][0] > 0.85)
                result.setText("Your eye result may be positive for retinopathy.\nConsider consulting a doctor.");
            else result.setText("You may have retinopathy.\nConsider consulting a doctor.");
            result.setTextColor(Color.parseColor("#fa0f0f"));
            retino = true;
        } else {
            result.setText("Your eye is healthy!");
            result.setTextColor(Color.parseColor("#00c410"));
        }
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);

        return resizedBitmap;
    }

    void loadFile() {
        try {
            tflite = new Interpreter(loadModelFile());
            classifyImage(image);
        } catch (Exception e) {
            makeToast("Error getting model.");
            Log.wtf("*Model Loading Error", e.toString());
        }
    }

    public MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    void setImage() {
        try {
            image = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//            image = MainActivity.handleSamplingAndRotationBitmap(getApplicationContext(), imageUri);
//            image = MainActivity.centerCrop(image);
            imageView.setImageBitmap(image);
        } catch (IOException e) {
            e.printStackTrace();
            makeToast("Error getting image");
            Log.wtf("*Uri to Bitmap Error", e.toString());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void shareWithEveryone() {
        Log.i("Share Results", "");
        String[] TO = {""};
        String[] CC = {""};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("application/image");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Eye Scanner");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi,\nHere are the results from the Eye Scanner.");

        try {
            startActivity(Intent.createChooser(emailIntent, "Share..."));
            finish();
            Log.i("Shared!", "");
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(ResultsActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendEmail() {
        Log.i("Send email", "");
        String[] TO = {""};
        String[] CC = {""};

        Intent emailIntent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.parse("mailto:?subject=" + "Retino Eye Scan" + "&body=" + "Hello, my eye test results on Retino" +
                "Scanner is attached below.\nThe test result may have been positive for Retinopathy." + "&to=" + "");
        emailIntent.setData(data);
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));

        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Your subject");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi,\nHere are the results from the Eye Scanner.");
        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
            Log.i("Finished sending email.", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(ResultsActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    Toast t;

    public void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG);
        t.show();
    }
}