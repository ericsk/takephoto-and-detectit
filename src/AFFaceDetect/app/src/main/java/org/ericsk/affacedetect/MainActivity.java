package org.ericsk.affacedetect;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Include the following imports to use blob APIs.

public class MainActivity extends AppCompatActivity implements OnClickListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    // Define the connection-string with your values
    public static final String storageConnectionString = "PUT_YOUR_STORAGE_CONNECTION_STRING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.take_photo);
        btn.setOnClickListener(this);
    }

    File photoFile;

    public void onClick(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("MainActivity", ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "org.ericsk.affacedetect",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    ProgressDialog dialog;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            dialog = ProgressDialog.show(MainActivity.this,
                    "上傳照片", "正在將照片上傳至 Azure Blob Storage...",true);
            new UploadFilesTask().execute(photoFile);
        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private class UploadFilesTask extends AsyncTask<File, Integer, Void> {
        protected Void doInBackground(File... photoFiles) {
            try {
                // Retrieve storage account from connection-string.
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                // Create the blob client.
                CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                // Get a reference to a container.
                // The container name must be lower case
                CloudBlobContainer container = blobClient.getContainerReference("input");

                // Create the container if it does not exist.
                container.createIfNotExists();

                for (int i = 0; i < photoFiles.length; ++i) {
                    File photoFile = photoFiles[i];
                    CloudBlockBlob blob = container.getBlockBlobReference(photoFile.getName());
                    blob.upload(new java.io.FileInputStream(photoFiles[0]), photoFile.length());
                }
            } catch (Exception e) {
                // Output the stack trace.
                e.printStackTrace();
            }
            dialog.dismiss();
            return null;
        }

        protected void onPostExecute() {
            Log.i("MainActivity", "Completed upload.");
        }

    }
}
