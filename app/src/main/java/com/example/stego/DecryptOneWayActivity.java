package com.example.stego;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.GenericArrayType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.example.stego.R.id.message;

public class DecryptOneWayActivity extends AppCompatActivity {
    Button btnTakePic,btnHide;
    ImageView imageView;
    private int GALLERY = 1;
    String message,key,decipherKey;
    EditText textKey,textDecipherKey;
    TextView textmessage;
    Bitmap bitmap;
    private static SecretKeySpec secretKey;
    private static byte[] cKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt_one_way);

        getSupportActionBar().setTitle("One Way Decryption");
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


        textKey = findViewById(R.id.key);
        textmessage=findViewById(R.id.message);
        textDecipherKey = findViewById(R.id.decipherKey);
        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                key = textKey.getText().toString();
                decipherKey = textDecipherKey.getText().toString();

                //showToast(message);
    //            showToast(key);
                message = decoded(bitmap);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && decipherKey.length()>0) {
                    message = decrypt(message,decipherKey);
                }

                textmessage.setText(message);
                Toast.makeText(DecryptOneWayActivity.this, message, Toast.LENGTH_SHORT).show();

            }


        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String decrypt(String strToDecrypt, String secret)
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
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


    private String decoded (Bitmap bit) {

         ByteBuffer byteBuffer = ByteBuffer.allocate(bit.getRowBytes() * bit.getHeight());
         bit.copyPixelsToBuffer(byteBuffer);
         byte[] byteArray = byteBuffer.array();
         int words = ByteBuffer.wrap(readFromTill(byteArray, 0, 4)).getInt();
         if (words < 0 || words > byteArray.length - 4) {
            return "No";
         }
         return new String(readFromTill(byteArray, 4, words + 4));

    }

    public byte[] readFromTill(byte[] bytes, int from, int to) {
        Log.e("Test",from+" "+to);
        byte[]read=new byte[(to - from)];
        for(int i=from;i<to;i++)
        {
            byte temp=(byte)0;
            for(int j=0 ; j<8 ; j++){
                if((bytes[(i*8)+j]&1)!=0){
                  temp=(byte)((1<<(7-j))|temp);
                }
            }
            read[i-from]=temp;

        }
        return read;
    }


    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        pictureDialog.setTitle("Choose Picture");
        String[] pictureDialogItems = {
                "Choose from gallery"};
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallery();
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
                    Toast.makeText(DecryptOneWayActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

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
