package com.example.cryptomoney;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import android.annotation.SuppressLint;
//import android.support.v7.app.AppCompatActivity;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cryptomoney.utils.Base64Utils;
import com.example.cryptomoney.utils.DBHelper;
import com.example.cryptomoney.utils.NfcUtils;
import com.example.cryptomoney.utils.RSAUtils;
import com.example.cryptomoney.utils.ReadDialog;
import com.example.cryptomoney.utils.WriteDialog;
import com.google.zxing.Result;
import com.google.zxing.activity.CaptureActivity;
import com.google.zxing.util.Constant;
import com.mysql.jdbc.PreparedStatement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;

import cn.memobird.gtx.GTX;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static java.lang.Math.min;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

public class MainActivity extends AppCompatActivity implements WriteDialog.MsgListener {

    private static final int RESULT_PASSSED = 3;
    private NfcUtils nfcUtils;
    private WriteDialog NFCDialog;
    private String qrstring;
    private String nfcstring;
    private String fullstring;
    private Boolean scanfinish = false;
    private Boolean showDialog = false;
    private Button qrscan;
    private String response;
//    private Button NFC_read;
//    private EditText scanreturn;
    private Button account;
    private Button transfer;
    private Button transaction;
    private Button qrgenerate;
    private Button crypto_tr;
    private Button crypto;
    private Button merchant_tr;
    private String addr;
    private SQLiteDatabase db;
    private Button logout;
    private Button rcv_free;
//    private Button execute;

    private Button request; //botton for merchants to request money from customers

    private Button Setting;

    private PrivateKey sk;
    private PublicKey pk;
    private String sk_enc;
    private SharedPreferences pref;
    private String type;

    private Integer account_id;
    private static String timePattern = "yyyy-MM-dd HH:mm:ss";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent_from_login = getIntent();
        account_id = intent_from_login.getIntExtra("account_id",0);
        nfcUtils = new NfcUtils(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)  {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);

        }

        DBHelper dbHelper = new DBHelper(this, "test.db", null, 3);
        db = dbHelper.getWritableDatabase();
        // get tokens from local db
        Cursor cursor = db.query("Tokens",null,null,null,null,null,null);
        if(cursor.moveToFirst()){
            do{
                addr = cursor.getString(cursor.getColumnIndex("addr"));
                Double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String token_sk_exp = cursor.getString(cursor.getColumnIndex("sk_exp"));
                String token_modulus = cursor.getString(cursor.getColumnIndex("N"));
                Log.d("MainActivity", "get offline tokens: " + addr + "  " + amount + "  " + account_id);
                try {
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    BigInteger token_N = new BigInteger(Base64Utils.decode(token_modulus));
                    BigInteger token_d = new BigInteger(Base64Utils.decode(token_sk_exp));
                    RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(token_N, token_d);
                    PrivateKey token_sk = keyFactory.generatePrivate(rsaPrivateKeySpec);

                    cipher.init(Cipher.ENCRYPT_MODE, token_sk);
                    String rcver_id = "ACCOUNT=" + account_id.toString();
                    byte[] enc_bytes = cipher.doFinal(rcver_id.getBytes());
                    String enc_id = Base64Utils.encode(enc_bytes);

                    // send addr, enc_id to server
                    final String cryptomoneyinRequest ="request=" + URLEncoder.encode("getencrypto") +
                            "&id_enc="+ URLEncoder.encode(enc_id)+"&addr="+ URLEncoder.encode(addr);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String response = PostService.Post(cryptomoneyinRequest);
                            if (response != null && response.indexOf("received") != -1 && !response.equals("-1.0 received")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // not delete token until it has been successfully received by the right account
                                        Common.showShortToast(MainActivity.this,response);
                                        db.delete("Tokens","addr=?",new String[] {addr});
                                    }
                                });
                            }
                        }
                    }).start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }while(cursor.moveToNext());
//            db.execSQL("Delete from Tokens");
        }
        cursor.close();
                //
//                // send to server
//                final String transferRequest = "request=" + URLEncoder.encode("useofflinetoken") + "&to_id=" + URLEncoder.encode(account_id.toString())
//                        + "&amount=" + URLEncoder.encode(amount.toString()) + "&addr=" + URLEncoder.encode(addr);
//
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        final String response = PostService.Post(transferRequest);
//                        if (response != null) {
//                            switch (response) {
//                                case "connection failed":
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            Common.showShortToast(MainActivity.this, "Connection Error");
//                                            finish();
//                                        }
//                                    });
//                                    break;
//                                default:
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            Common.showShortToast(MainActivity.this, "Offline token added successful." + response);
//                                        }
//                                    });
//                                    break;
//                            }
//                        }
//
//                    }
//                }).start();

        qrscan = (Button) findViewById(R.id.qrscan);
//        NFC_read = (Button) findViewById(R.id.nfctag);
        account = (Button)  findViewById(R.id.account_info);
//        transfer = (Button) findViewById(R.id.transfer);
//        transaction = (Button) findViewById(R.id.tr_detail);
//        qrgenerate = (Button) findViewById(R.id.qrgenerate);
        crypto = (Button) findViewById(R.id.crypto);
        crypto_tr = (Button) findViewById(R.id.cryptotr_detail);
//        execute = (Button) findViewById(R.id.execute);
//        scanreturn = (EditText) findViewById(R.id.scan_result);
        request = (Button) findViewById(R.id.request);
        Setting = (Button) findViewById(R.id.setting);
        merchant_tr = (Button) findViewById(R.id.merchanttr_detail);
        logout = (Button) findViewById(R.id.logout);
        rcv_free = (Button) findViewById(R.id.rcv_free);

//        type = intent_from_login.getStringExtra("type");

        rcv_free.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, Constant.REQ_PERM_CAMERA);
                } else {
                    openCamera();
                }
            }
        });

        crypto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String cryptotransactionRequest ="request=" + URLEncoder.encode("cryptotransaction") + "&id="+ URLEncoder.encode(account_id.toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String response = PostService.Post(cryptotransactionRequest);
                        if (response != null) {

                            try {
                                List<CryptoRecord> recordList = new ArrayList<>();
//                                SimpleDateFormat sdf = new SimpleDateFormat(timePattern);
                                JSONArray jsonArray = new JSONArray(response);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    CryptoRecord record = new CryptoRecord();
                                    record.setIndex(jsonObject.getInt("index"));
                                    record.setAddr(jsonObject.getString("address"));
                                    record.setTime(jsonObject.getString( "time"));
                                    record.setValue(jsonObject.getDouble("value"));
                                    recordList.add(record);
                                }

                                Intent intent = new Intent(MainActivity.this,CryptoActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("recordList", (Serializable) recordList);
                                bundle.putInt("account_id",account_id);
//                                bundle.putString("type",type);
                                intent.putExtras(bundle);
                                startActivity(intent);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
//                Intent intent = new Intent(MainActivity.this,CryptoActivity.class); // 启动TransferActivity,传入account_id
//                intent.putExtra("account_id",account_id);
//                startActivity(intent);
            }
        });

        crypto_tr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String cryptotransactionRequest ="request=" + URLEncoder.encode("cryptotransaction") + "&id="+ URLEncoder.encode(account_id.toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String response = PostService.Post(cryptotransactionRequest);
                        if (response != null) {

                            try {
                                List<CryptoRecord> recordList = new ArrayList<>();
//                                SimpleDateFormat sdf = new SimpleDateFormat(timePattern);
                                JSONArray jsonArray = new JSONArray(response);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    CryptoRecord record = new CryptoRecord();
                                    record.setIndex(jsonObject.getInt("index"));
                                    record.setAddr(jsonObject.getString("address"));
                                    record.setTime(jsonObject.getString( "time"));
                                    record.setValue(jsonObject.getDouble("value"));
                                    recordList.add(record);
                                }

                                Intent intent = new Intent(MainActivity.this,CryptoTransactionActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("recordList", (Serializable) recordList);
                                bundle.putInt("account_id",account_id);
                                bundle.putString("type",type);
                                intent.putExtras(bundle);
                                startActivity(intent);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });

        merchant_tr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String merchanttransactionRequest ="request=" + URLEncoder.encode("merchanttransaction") + "&id="+ URLEncoder.encode(account_id.toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String response = PostService.Post(merchanttransactionRequest);
                        if (response != null) {

                            try {
                                List<CryptoRecord> recordList = new ArrayList<>();
//                                SimpleDateFormat sdf = new SimpleDateFormat(timePattern);
                                JSONArray jsonArray = new JSONArray(response);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    CryptoRecord record = new CryptoRecord();
                                    record.setIndex(jsonObject.getInt("index"));
//                                    record.setAddr(jsonObject.getString("cipher"));
                                    record.setMerchant(jsonObject.getString("merchant"));
                                    record.setCiphertext(jsonObject.getString( "ciphertext"));
                                    record.setTime(jsonObject.getString( "time"));
                                    record.setValue(jsonObject.getDouble("value"));
                                    recordList.add(record);
                                }

                                Intent intent = new Intent(MainActivity.this,MerchanttransactionActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("recordList", (Serializable) recordList);
                                bundle.putInt("account_id",account_id);
                                bundle.putString("type",type);
                                intent.putExtras(bundle);
                                startActivity(intent);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });

//        qrgenerate.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                startActivity(new Intent(MainActivity.this, QRgeneratorActivity.class));
//            }
//        });
        logout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        qrscan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Intent intent_to_mode = new Intent(MainActivity.this,ReceiveActivity.class);
                intent_to_mode.putExtra("account_id",account_id);
                startActivity(intent_to_mode);

            }
        });

//        NFC_read.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//
//                startActivity(new Intent(MainActivity.this, NFCRWActivity.class));
//            }
//        });

        account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String accountInfoRequest ="request=" + URLEncoder.encode("accountinfo") +
                        "&id="+ URLEncoder.encode(account_id.toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        final String response = PostService.Post(accountInfoRequest);
                        if (response != null) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                int id = jsonObject.getInt("id");
                                String username = jsonObject.getString("username");
                                Log.d("MainActivity",username);
                                Double balance = jsonObject.getDouble("balance");
                                String email = jsonObject.getString("email");
                                String cellphone = jsonObject.getString("cellphone");

                                Intent intent_to_account = new Intent(MainActivity.this, AccountActivity.class); // 启动AccountActivity传入用户信息
                                intent_to_account.putExtra("id",id);
                                intent_to_account.putExtra("username",username);
                                intent_to_account.putExtra("balance", balance);
                                intent_to_account.putExtra("email",email);
                                intent_to_account.putExtra("cellphone",cellphone);
                                startActivity(intent_to_account);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });

//        transfer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this,TransferActivity.class); // 启动TransferActivity,传入account_id
//                intent.putExtra("account_id",account_id);
//                startActivity(intent);
//            }
//        });

//        transaction.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                final String transactionRequest ="request=" + URLEncoder.encode("transaction") + "&id="+ URLEncoder.encode(account_id.toString());
//
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        final String response = PostService.Post(transactionRequest);
//                        if (response != null) {
//
//                            try {
//                                List<Record> recordList = new ArrayList<>();
//                                SimpleDateFormat sdf = new SimpleDateFormat(timePattern);
//                                JSONArray jsonArray = new JSONArray(response);
//                                for (int i = 0; i < jsonArray.length(); i++) {
//                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
//                                    Record record = new Record();
//                                    record.setIndex(jsonObject.getInt("index"));
//                                    record.setFrom(jsonObject.getInt("from_account"));
//                                    record.setTo(jsonObject.getInt("to_account"));
//                                    record.setTime(jsonObject.getString( "time"));
//                                    record.setValue(jsonObject.getDouble("value"));
//                                    recordList.add(record);
//                                }
//
//                                Intent intent = new Intent(MainActivity.this,TransactionActivity.class);
//                                Bundle bundle = new Bundle();
//                                bundle.putSerializable("recordList", (Serializable) recordList);
//                                bundle.putInt("account_id",account_id);
//                                intent.putExtras(bundle);
//                                startActivity(intent);
//
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }).start();
//
//            }
//        });

        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,RequestActivity.class); // 启动TransferActivity,传入account_id
                intent.putExtra("account_id",account_id);
                startActivity(intent);
            }
        });

        Setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,SettingActivity.class); // 启动TransferActivity,传入account_id
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {  //权限请求结果回调
        if (requestCode == Constant.REQ_PERM_CAMERA && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
        else {
            Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQ_QR_CODE && resultCode == RESULT_PASSSED) {
            qrstring = "";
            showDialog = true;
            showSaveDialog();
        }

        if (requestCode == Constant.REQ_QR_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            qrstring = bundle.getString(Constant.INTENT_EXTRA_KEY_QR_SCAN);
            System.out.println("qrstring= "+qrstring);

            // only QR
            if (qrstring.indexOf("N=") == 0 && qrstring.indexOf("&d=") != -1 && qrstring.indexOf("&addr=") != -1) {
                fullstring = qrstring;
                scanfinish = true;
                sendtokenaddr();
            }

            // if qr string not begin with "N=" but begin with "N", then NFC is needed, otherwise wrong string
            else if (qrstring.indexOf("N=") != 0 && qrstring.indexOf("N") == 0 ) {
                showDialog = true;
                showSaveDialog();
            }
//
            else {
                Common.showShortToast(MainActivity.this, "Invalid message,scan again");
//                openCamera();
            }
        }
    }

    private void openCamera() {
        try {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            intent.putExtra("from","login");
            startActivityForResult(intent, Constant.REQ_QR_CODE);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Exit confirmation");
        dialog.setMessage("Are you sure to exit?");
        dialog.setCancelable(true);
        dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                GTX.exitApp();
                finish();
            }
        });
        dialog.setNegativeButton("No", null);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
            default:
        }
        return true;
    }

    private String getfullstring(String qrstring,String nfcstring) {
        String fullstring = "";
        Integer minlength = min(qrstring.length(),nfcstring.length());
        int i;
        for (i=0; i<minlength; i++) {
            fullstring += qrstring.charAt(i);
            fullstring += nfcstring.charAt(i);
        }
        if (qrstring.length() > nfcstring.length()) {
            fullstring += qrstring.charAt(i);
        }
        else if (qrstring.length() < nfcstring.length()) {
            fullstring += nfcstring.charAt(i);
        }
        return fullstring;
    }

    @Override
    protected void onNewIntent(Intent intent){
        if (showDialog) {
            super.onNewIntent(intent);
            nfcstring = nfcUtils.readMessage(intent);
            dissDialog();
//            Common.showShortToast(this, "NFC reading successfully.");
            if (qrstring.equals("")) fullstring = nfcstring;
            else fullstring = getfullstring(qrstring,nfcstring);
            System.out.println("full string after NFC= "+ fullstring);
            scanfinish = true;
            showDialog = false;
//            Common.showShortToast(this, "Read NFC successfully.");
            sendtokenaddr();
        }
    }

    private void showSaveDialog() {
//        System.out.println("into showsavedialog");
        if (getSupportFragmentManager().findFragmentByTag("mWriteDialog") == null) {
            NFCDialog = new WriteDialog();
            System.out.println(NFCDialog);
            NFCDialog.show(getSupportFragmentManager(), "mWriteDialog");
//            System.out.println("show nfcdialog");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcUtils.enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcUtils.disableForegroundDispatch();
    }

    private void dissDialog() {
        if (NFCDialog != null &&
                NFCDialog.getDialog() != null &&
                NFCDialog.getDialog().isShowing()) {
            NFCDialog.dismiss();
        }
    }

    public void sendtokenaddr() {
        if (scanfinish) {
            // get fullstring and extract sk, addr
            System.out.println("full string= " + fullstring);
            if (fullstring.indexOf("N=") == 0 && fullstring.indexOf("&d=") != -1 && fullstring.indexOf("&addr=") != -1) {
                String N_base64 = fullstring.split("N=")[1].split("&d=")[0];
                BigInteger N = new BigInteger(Base64Utils.decode(N_base64));
                String d_base64 = fullstring.split("&d=")[1].split("&addr=")[0];
                BigInteger d = new BigInteger(Base64Utils.decode(d_base64));

                String token_addr = fullstring.split("&addr=")[1];
                System.out.println("token addr= " + token_addr);

                try {
                    // extract token sk from N,d
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(N, d);
                    PrivateKey token_sk = keyFactory.generatePrivate(rsaPrivateKeySpec);

                    // encrypt rcver account id  with token sk -> enc_id
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, token_sk);
                    String rcver_id = "ACCOUNT=" + account_id.toString();
                    byte[] enc_bytes = cipher.doFinal(rcver_id.getBytes());
                    String enc_id = Base64Utils.encode(enc_bytes);

                    // final string sent to server: {contract_addr,{token_addr,{rcver_id}_tokensk_contractsk}}
                    final String getfreeencryptoRequest = "request=" + URLEncoder.encode("getfreeencrypto") +
                            "&addr=" + URLEncoder.encode(token_addr) + "&id_enc=" + URLEncoder.encode(enc_id);
                    System.out.println("getfreeencryptoRequest= " + getfreeencryptoRequest);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // server response will be "enough" or "not enough"
                            response = PostService.Post(getfreeencryptoRequest);
                            System.out.println("response= " + response);
                            if (response != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Common.showLongToast(MainActivity.this, response);
                                    }
                                });

                            }
                        }
                    }).start();

                } catch (Exception e) {
                    e.printStackTrace();
                    Common.showShortToast(MainActivity.this, response);
                }
            } else {
                Common.showShortToast(MainActivity.this, "Invalid message,scan again");
            }

        }
    }

    @Override
    public void cancelresult(Boolean iscancel) {
        if (iscancel) {
        }
    }


}