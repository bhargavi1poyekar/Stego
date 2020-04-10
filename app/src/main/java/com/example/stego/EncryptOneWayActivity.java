package com.example.stego;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class EncryptOneWayActivity extends AppCompatActivity {
    Button btnTakePic,btnHide,btnShare;
    ImageView imageView;
    private static final String IMAGE_DIRECTORY = "/Pictures";
    private int GALLERY = 1, CAMERA = 2;
    String message,key,pathname;
    EditText textMessage,textKey,cipherKey;
    Boolean isShare=false;

    Bitmap bitmap;
    Bitmap bitout;

    private static final String endFlag="(#*BUREK*#)";
    private static final String passwordEndFlag="$!]";
    private static final String passwordStartFlag="[!$";
    private static final String startFlag="(#*CEVAP*#)";

    /*public enum PhotoStatus{
        NOT_ENCODED,
        ENCODED,
        PASSWORD_ENCODED,
        UNKNOWN
    }*/



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt_one_way);
        getSupportActionBar().setTitle("One Way Encryption");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        requestMultiplePermissions();
        btnTakePic = findViewById(R.id.btnTakePic);
        btnHide = findViewById(R.id.hideButton);
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPictureDialog();
            }
        });
        imageView = findViewById(R.id.image);

        textMessage= findViewById(R.id.textMessage);
        textKey = findViewById(R.id.key);
        cipherKey = findViewById(R.id.cipherKey);


        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                message = textMessage.getText().toString();
                key = textKey.getText().toString();
                showToast(message);
                showToast(key);
                modify (message,key);

            }


        });
        btnShare=findViewById(R.id.buttonShareImage);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareImage();
        }

        });
    }

    private void shareImage() {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        if(isShare)
        {
            Uri screenshotUri = Uri.parse(pathname);

            sharingIntent.setType("image/png");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
            startActivity(Intent.createChooser(sharingIntent, "Share image using"));
        }


    }

    private void modify(String rawMessage, String password) {
        String wrappedPassword="";
        if(!password.isEmpty())
        {
            wrappedPassword=new StringBuilder(passwordStartFlag).append(password).append(passwordEndFlag).toString();

        }
        String newMessage=new StringBuilder(startFlag).append(wrappedPassword).append(rawMessage).append(endFlag).toString();
        encoded(newMessage);
    }




    private void encoded(String message) {
        if(this.bitmap!=null)
        {
            this.btnHide.setEnabled(false);
            ByteBuffer byteBuffer= ByteBuffer.allocate(this.bitmap.getRowBytes()*this.bitmap.getHeight());
            this.bitmap.copyPixelsToBuffer(byteBuffer);
            byte[] byteArray= ( byteBuffer).array();
            byte[] textBytes=message.getBytes();
            ByteBuffer bb=ByteBuffer.allocate(4);
            bb.putInt(message.length());
            byte[]prefix=bb.array();
            byte[]result=new byte[(prefix.length+textBytes.length)];
            System.arraycopy(prefix,0,result,0,prefix.length);
            System.arraycopy(textBytes,0,result,prefix.length,textBytes.length);
            Log.e("Test", Arrays.toString(result));
            if(((double)textBytes.length)>=Math.floor(((double)byteArray.length)/8.0d)){
                Log.e("Test","message too long");
                return;
            }
            StringBuilder sb=new StringBuilder(textBytes.length);
            for(int i=0;i<result.length;i++)
            {
                byte temp=result[i];
                int j=0;
                while(j<8)
                {
                    int i2;
                    if(((1<<(7-j))&temp)!=0)
                    {
                        i2=(i*8)+j;
                        byteArray[i2]=(byte)(byteArray[i2]|1);
                        if(i==0 && j!=0)
                        {
                        }
                    }
                    else{
                        i2=(i*8)+j;
                        byteArray[i2]=(byte)(byteArray[i2]& -2);
                        if(i==0 && j!=0){
                        }
                    }
                    j++;
                }
            }
            Bitmap.Config configBmp=Bitmap.Config.valueOf(this.bitmap.getConfig().name());
            this.bitout=Bitmap.createBitmap(this.bitmap.getWidth(),this.bitmap.getHeight(),configBmp);
            this.bitout.copyPixelsFromBuffer(ByteBuffer.wrap(byteArray));
            saveImage(bitout);
            isShare=true;


        }

    }

    private void showToast(String message) {
        Toast.makeText(EncryptOneWayActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Choose from gallery",
                "Capture photo from camera"};
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallery();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;

                        }
                    }
                });
        pictureDialog.show();
    }

    public void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                     bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    //String path = saveImage(bitmap);
                    //Toast.makeText(EncryptOneWayActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
                    imageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(EncryptOneWayActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        } else if (requestCode == CAMERA) {
             bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
            //saveImage(thumbnail);
            //Toast.makeText(EncryptOneWayActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }
    }

    public String saveImage(Bitmap myBitmap) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();


        File wallpaperDirectory = new File(Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".png");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            myBitmap.compress(Bitmap.CompressFormat.PNG, 100, fo);


            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/png"}, null);
            fo.flush();
            fo.close();
            Log.d("TAG", "File Saved::---&gt;" + f.getAbsolutePath());
            pathname=f.getAbsolutePath();
            Toast.makeText(EncryptOneWayActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return "";
    }

    private void  requestMultiplePermissions(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            //openSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }
}

