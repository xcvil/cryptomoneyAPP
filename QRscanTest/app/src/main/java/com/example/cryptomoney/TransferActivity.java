package com.example.cryptomoney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.net.URLEncoder;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.sql.Types.INTEGER;

public class TransferActivity extends AppCompatActivity {

    // 定义控件和全局变量初始化
    private ImageButton back;
    private Button transfer;
    private Integer account_id;
    private Connection conn = null;

    public final static int TYPE_CONN_FAILED = -1;
    public final static int TYPE_TR_SUCCESS = 1;
    public final static int TYPE_AMOUNT_FAILED = 0;
    public final static int TYPE_ID_FAILED = -2;
    public final static int TYPE_SELF_FAILED = -3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        Intent intent_from_main = getIntent(); // 获取account_id
        account_id = intent_from_main.getIntExtra("account_id", 0);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        transfer = (Button) findViewById(R.id.transfer);
        transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText to_account = (EditText) findViewById(R.id.to_account);
                EditText amount = (EditText) findViewById(R.id.amount);

                boolean isdouble = isNumeric(amount.getText().toString());
                boolean isint = isInteger(to_account.getText().toString());
                if (isdouble != true || isint != true)
                    Common.showShortToast(TransferActivity.this, "Invalid input");
                else {
                    final Double value = Double.parseDouble(amount.getText().toString());
                    final Integer to_account_id = Integer.parseInt(to_account.getText().toString());  // 定义ExeTransaction传参
                    final Integer from_account_id = account_id;
//                Log.d("TransferActivity","to account:" + to_account_id + "amount: " + value);

                    final String transferRequest = "request=" + URLEncoder.encode("transfer") + "&to_account=" + URLEncoder.encode(to_account_id.toString())
                            + "&value=" + URLEncoder.encode(value.toString()) + "&from_account=" + URLEncoder.encode(from_account_id.toString());

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String response = PostService.Post(transferRequest);
                            if (response != null) {
                                int tr_result = Integer.parseInt(response);
//                            Log.d("TransferActivity",""+tr_result);
                                switch (tr_result) {
                                    case TYPE_TR_SUCCESS:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Common.showShortToast(TransferActivity.this, "transfer succeeded");
                                                finish();
                                            }
                                        });
                                        break;
                                    case TYPE_ID_FAILED:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Common.showShortToast(TransferActivity.this, "Account Id doesn't exist");
                                            }
                                        });
                                        break;
                                    case TYPE_SELF_FAILED:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Common.showShortToast(TransferActivity.this, "Can't transfer to yourself");
                                            }
                                        });
                                        break;
                                    case TYPE_AMOUNT_FAILED:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Common.showShortToast(TransferActivity.this, "Not enough balance");
                                            }
                                        });
                                        break;
                                    default:
                                        break;
                                }
                            }

                        }
                    }).start();
                }

            }
        });
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

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]+[.]{0,1}[0-9]*[dD]{0,1}");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isInteger(String str) {
        for (int i = 0; i < str.length(); i++) {
            System.out.println(str.charAt(i));
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}