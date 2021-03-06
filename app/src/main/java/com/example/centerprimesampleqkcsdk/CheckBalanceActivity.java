package com.example.centerprimesampleqkcsdk;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.centerprime.quarkchainsdk.QKCManager;
import com.centerprime.quarkchainsdk.quarck.Numeric;
import com.centerprime.quarkchainsdk.util.BalanceUtils;
import com.example.centerprimesampleqkcsdk.databinding.ActivityCheckBalanceBinding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Collections;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CheckBalanceActivity extends AppCompatActivity {
    ActivityCheckBalanceBinding binding;

    /**
     * @param chain - collection of chain numbers to choose
     */
    String[] chain = {"0", "1", "2", "3", "4", "5", "6"};

    /**
     * @param chainID - Initialize chain ID. Default is 0
     */
    int chainID = Integer.valueOf(chain[0]);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_check_balance);

        QKCManager qkcManager = QKCManager.getInstance();
        qkcManager.init("http://jrpc.mainnet.quarkchain.io:38391");

        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, chain);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spin.setAdapter(aa);

        /**
         * This spinner chooses chain of QKC wallet address
         */
        binding.spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chainID = Integer.valueOf(chain[position]);
                System.out.println(chainID);

                if (TextUtils.isEmpty(binding.address.getText())){
                    return;
                }
                /**
                 * @param address - ethereum address
                 */
                String address = binding.address.getText().toString();
                if (!address.startsWith("0x")) {
                    address = "0x" + address;
                }

                /**
                 * @param qckWalletAddress - convert ethereum address to QKC wallet address
                 */
                String qckWalletAddress = qkcManager.getQCKAddress(address,CheckBalanceActivity.this);

                /**
                 * @param chainBasedAddress - convert qckWalletAddress to QKC chainBased address
                 */
                String chainBasedAddress =qkcManager.getQCKAddressByChainIdAndShardId(qckWalletAddress,chainID,0,CheckBalanceActivity.this);
                binding.quarkAddress.setText(chainBasedAddress);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.checkBtn.setOnClickListener(v -> {
            String address = binding.address.getText().toString();
            if (!address.startsWith("0x")) {
                address = "0x" + address;
            }

            /**
             * Using this balanceInEth function you can check balance of provided walletAddress.
             *
             * @param walletAddress - which user want to check it's balance
             * @param context - activity context
             *
             * @return if the function completes successfully returns balance of provided wallet address or returns error name
             */

            String qckWalletAddress = qkcManager.
                    getQCKAddress(address,this);

            String chainBasedAddress =qkcManager.getQCKAddressByChainIdAndShardId(qckWalletAddress,chainID,0,this);


            qkcManager.getQCKBalance(chainBasedAddress, this)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(accountData -> {
                        /**
                         * if function successfully completes result can be caught in this block
                         */
                        BigInteger balance = BigInteger.ZERO;
                        if (!accountData.getPrimary().getBalances().isEmpty()) {
                            balance = Numeric.toBigInt(accountData.getPrimary().getBalances().get(0)
                                    .getBalance());
                        }
                        BigDecimal qkcBalance = BalanceUtils.weiToEth(balance);
                        String pattern = "###,###.########";
                        DecimalFormat decimalFormat = new DecimalFormat(pattern);
                        System.out.println("**** **** " + decimalFormat.format(qkcBalance));
                        binding.balanceTxt.setText("QKC balance: " + decimalFormat.format(qkcBalance));
                        //   binding.balanceTxt.setText("QKC balance: " + accountData.getPrimary().getBalances().get(1));

                    }, error -> {
                        /**
                         * if function fails error can be caught in this block
                         */
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();

                    });
        });
    }

}
