package app.ij.hackricexretinopathyapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;

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

import java.io.IOException;

public class ResultsActivity extends AppCompatActivity {

    TextView title;
    Button share;
    Bitmap image;
    ImageView imageView;
    Uri imageUri;

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
    }

    void setImage(){
        try {
            image = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            image = MainActivity.handleSamplingAndRotationBitmap(getApplicationContext(), imageUri);
            image = MainActivity.centerCrop(image);
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
        Uri data = Uri.parse("mailto:?subject=" + "Eye Scan" + "&body=" + "Hello, my eye test results on Retino" +
                "Scanner are shown below. Please check it out!" + "&to=" + "");
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