package com.example.stego;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ImageDecryptActivity extends AppCompatActivity {

    Button btnTakePic,btnDecode;
    ImageView imageView;
    private int GALLERY = 1;
    String message,key,deCipher;
    TextInputEditText textKey,deCipherKey;
    TextInputLayout cipherLayout;
    CheckBox twoWay;
    TextView textMessage;

    Bitmap bitmap;

    Boolean isCipher=false;

    private static SecretKeySpec secretKey; //for deciphering
    private static byte[] cKey;             //for deciphering


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_decrypt);

        getSupportActionBar().setTitle("Decrypt");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        requestMultiplePermissions();

        btnTakePic = findViewById(R.id.btnTakePic);
        textMessage = findViewById(R.id.textMessage);

        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPictureDialog();

            }
        });
        imageView = findViewById(R.id.image);
        textKey = findViewById(R.id.key);

        deCipherKey = findViewById(R.id.deCipherKey);

        cipherLayout = findViewById(R.id.cipherLayout);
        cipherLayout.setVisibility(View.GONE);

        twoWay = findViewById(R.id.checkBox);
        twoWay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCipher = ((CheckBox) view).isChecked();
                if(isCipher) {
                    Toast.makeText(ImageDecryptActivity.this, "Checked", Toast.LENGTH_SHORT).show();
                    cipherLayout.setVisibility(View.VISIBLE);
                    deCipherKey.setText("");
                }
                if(!isCipher){
                    Toast.makeText(ImageDecryptActivity.this, "Unchecked", Toast.LENGTH_SHORT).show();
                    cipherLayout.setVisibility(View.GONE);
                    deCipherKey.setText("");
                }

            }
        });


        btnDecode = findViewById(R.id.btnDecode);
        btnDecode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                key = textKey.getText().toString();
                if(isCipher)
                    deCipher = deCipherKey.getText().toString();

                if(key.length()>7 || key.length()<1 || (isCipher && (deCipher.length()<1 || deCipher.length()>7))){
                    if (key.length() > 7)
                        textKey.setError("Key should be less than 7 characters");
                    if(key.length()<1)
                        textKey.setError("Key should be minimum 1 character long");
                    if (isCipher && deCipher.length() < 1 )
                        deCipherKey.setError("Decipher Key cannot be Empty");
                    if (isCipher && deCipher.length() > 7 )
                        deCipherKey.setError("Decipher Key should be less than 7 characters ");
                }

                else {
                    message = decoded(bitmap);
                    if (isCipher && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        message = decrypt(message, deCipher);
                        //Toast.makeText(ImageDecryptActivity.this, message, Toast.LENGTH_SHORT).show();

                        if (message != null) {
                            String msg;
                            msg = modify(message, key);
                            message=msg;
                            Toast.makeText(ImageDecryptActivity.this, message, Toast.LENGTH_SHORT).show();

                            if (message.equals("Invalid Key")) {
                                deCipherKey.setError("Invalid Key");
                                textKey.setError("Invalid Key");
                                message="Message Not Found";
                                textMessage.setText(message);
                                Log.e("Test", "Stego Key Wrong");
                            }
                            else {
                                Log.d("Test", "Main"+msg);
                                Toast.makeText(ImageDecryptActivity.this, message, Toast.LENGTH_SHORT).show();
                                textMessage.setText(message);
                            }
                        }
                        else{
                            deCipherKey.setError("Invalid Key");
                            textKey.setError("Invalid Key");
                            message="Message Not Found";
                            textMessage.setText(message);
                            Log.e("Test", "Cipher Key Wrong");
                        }


                }
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

    private String modify(String message, String Key){
        //Log.d("Test","Flag0");
        int i=0;
        String pass="";
        StringBuilder sb = new StringBuilder(pass);
        String msg = "";
        StringBuilder m = new StringBuilder(msg);
        int length = message.length();
        //Log.d("Test","Flag1");

        while(i<length && message.charAt(i)!='_'){
            pass = sb.append(message.charAt(i)).toString();
            i++;
            //Log.v("Test","While"+i);
        }
        if(i<length){
            i++;
            if(pass.equals(key)){
                while(message.charAt(i)!=']'){
                    msg = m.append(message.charAt(i)).toString();
                    i++;
                }
                Log.d("Test",msg);
                return msg;
            }
        }

        return "Invalid Key";

    }

    private String decoded (Bitmap bit) {

        if(this.bitmap!=null){
            ByteBuffer byteBuffer = ByteBuffer.allocate(bit.getRowBytes() * bit.getHeight());
            bit.copyPixelsToBuffer(byteBuffer);
            byte[] byteArray = byteBuffer.array();
            int words = ByteBuffer.wrap(readFromTill(byteArray, 0, 4)).getInt();
            if (words < 0 || words > byteArray.length - 4) {
                return null;
            }
            Log.d("Test","Success(Decode)");
            return new String(readFromTill(byteArray, 4, words + 4));
        }

        return null;
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
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        textKey.setText("");
        deCipherKey.setText("");

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
                    Toast.makeText(ImageDecryptActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
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
