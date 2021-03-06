package com.example.cryptomoney;

import android.content.Intent;
import android.graphics.Color;
import android.nfc.FormatException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.example.cryptomoney.utils.Constant;
import com.example.cryptomoney.utils.Logger;
import com.example.cryptomoney.utils.NfcUtils;


import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class NFCRWActivity extends AppCompatActivity {

    private RadioButton btnWrite, btnRead;
    private RadioGroup group;
    private NfcUtils nfcUtils;
    private String TAG = NFCRWActivity.class.getSimpleName();
    private int selectMode = Constant.NFC_READ;
    private String msgWrite = null;
    private EditText nfc_txt;
    private ImageButton back;

    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcrw);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)  {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }


        btnWrite = (RadioButton) findViewById(R.id.write);
        btnRead = (RadioButton) findViewById(R.id.read);
        group = (RadioGroup) findViewById(R.id.group);
        nfc_txt = (EditText) findViewById(R.id.nfc_txt);
        nfcUtils = new NfcUtils(this);

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()  {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.write:
                        nfc_txt.setText("");
                        nfc_txt.setHint("input string here and near NFC tag");
                        selectMode = Constant.NFC_WRITE_NDEF;
                        break;
                    case R.id.read:
                        nfc_txt.setText("");
                        nfc_txt.setHint("NFC content will be displayed here");
                        selectMode = Constant.NFC_READ;
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switch (selectMode) {
            case Constant.NFC_WRITE_NDEF:  // write to NFC
                msgWrite = nfc_txt.getText().toString();
                if (TextUtils.isEmpty(msgWrite)) {
                    Toast.makeText(this,"Input string can't be empty.",Toast.LENGTH_SHORT).show();
                    break;
                }
                try {
                    nfcUtils.writeNFCToTag(msgWrite, intent);
//                    Log.d("NFCRWActivity","finish writing");
                    toast = Toast.makeText(this,"write to NFC successfully.",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                } catch (IOException e) {
                    toast = Toast.makeText(this,"write to NFC failed.",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
//                    Log.d("NFCRWActivity","write to NFC failed：" + e.getMessage());
                } catch (FormatException e) {
                    toast = Toast.makeText(this,"write to NFC failed.",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
//                    Log.d("NFCRWActivity","write to NFC failed：" + e.getMessage());
                } finally {
                    msgWrite = null;
                }
                break;

            case Constant.NFC_READ:
                String message = nfcUtils.readMessage(intent);
                toast = Toast.makeText(this,"Read NFC successfully.",Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER,0,0);
                toast.show();
//                mDialog = new ReadDialog();
                nfc_txt.setText(message);

//                Logger.e(TAG, "nfcMessage:" + message);
                break;

            default:
                Toast.makeText(this,"Please choose NFC mode.",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Logger.e(TAG, "onResume");
        nfcUtils.enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        Logger.e(TAG, "onPause");
        nfcUtils.disableForegroundDispatch();
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

}
