package com.example.stego;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class ImageEncryptActivity extends AppCompatActivity {
    Button btnTakePic,btnHide,btnShare;
    ImageView imageView;
    String message,key,cipher,pathname;
    TextInputEditText textMessage,textKey,cipherKey;
    TextInputLayout cipherLayout;
    CheckBox twoWay;
    private static final String IMAGE_DIRECTORY = "/Pictures";
    private int GALLERY = 1, CAMERA = 2;
    Bitmap bitmap,bitOut;

    Boolean isCipher=false;
    Boolean isShare=false;

    private static SecretKeySpec secretKey; //for ciphering
    private static byte[] cKey;              //for ciphering


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_encrypt);

        getSupportActionBar().setTitle("Encrypt");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        requestMultiplePermissions();

        btnTakePic = findViewById(R.id.btnTakePic);

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

        cipherLayout = findViewById(R.id.cipherLayout);
        cipherLayout.setVisibility(View.GONE);

        twoWay = findViewById(R.id.checkBox);
        twoWay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCipher = ((CheckBox) view).isChecked();
                if(isCipher) {
                    Toast.makeText(ImageEncryptActivity.this, "Checked", Toast.LENGTH_SHORT).show();
                    cipherKey.setText("");
                    cipherLayout.setVisibility(View.VISIBLE);
                }
                if(!isCipher){
                    Toast.makeText(ImageEncryptActivity.this, "Unchecked", Toast.LENGTH_SHORT).show();
                    cipherLayout.setVisibility(View.GONE);
                    cipherKey.setText("");
                }

            }
        });



        btnHide = findViewById(R.id.hideButton);
        btnHide.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                message = textMessage.getText().toString();
                key = textKey.getText().toString();
                if(isCipher)
                    cipher = cipherKey.getText().toString();

                if(key.length() > 7 || key.length()<1 || message.length() < 1 ||(isCipher &&(cipher.length() < 1 || cipher.length() >7))){
                    if (key.length() > 7)
                        textKey.setError("Key should be less than 7 characters");

                    if(key.length()<1)
                        textKey.setError("Key should be minimum 1 character long");

                    if (message.length() < 1)
                        textMessage.setError("Message cannot be Empty");

                    if (isCipher && cipher.length() < 1 )
                        cipherKey.setError("Cipher Key cannot be Empty");

                    if (isCipher && cipher.length() > 7 )
                        cipherKey.setError("Cipher Key should be less than 7 characters ");

                }
                else{
                    message = modify(message,key);
                    if(isCipher)
                        message = encrypt(message,cipher);

                    encoded(message);
                }

                //showToast(message);
                //showToast(key);

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String encrypt(String strToEncrypt, String secret) {
        try
        {
            setKey(secret);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        }
        catch (Exception e)
        {
            Toast.makeText(ImageEncryptActivity.this, "Error while ciphering", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void setKey(String myKey) {
        MessageDigest sha;
        try {
            cKey = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            cKey = sha.digest(cKey);
            cKey = Arrays.copyOf(cKey, 16);
            secretKey = new SecretKeySpec(cKey, "AES");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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

    private String modify(String message, String key) {
        String Connector = "_";
        String end = "]";
        return new StringBuilder(key).append(Connector).append(message).append(end).toString();
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
            this.bitOut=Bitmap.createBitmap(this.bitmap.getWidth(),this.bitmap.getHeight(),configBmp);
            this.bitOut.copyPixelsFromBuffer(ByteBuffer.wrap(byteArray));
            saveImage(bitOut);
            isShare=true;


        }

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
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        textKey.setText("");
        cipherKey.setText("");
        textMessage.setText("");
        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        textKey.setText("");
        cipherKey.setText("");
        textMessage.setText("");

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
                    imageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ImageEncryptActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        } else if (requestCode == CAMERA) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);

        }
    }

    public void saveImage(Bitmap myBitmap) {

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
            Toast.makeText(ImageEncryptActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
            f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

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




