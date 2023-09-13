package com.example.mdp_group18;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import java.nio.charset.Charset;


public class BluetoothChatTabFragment extends Fragment {

    private static final String TAG = "BluetoothChatTabFragment";
    private SharedPreferences sharedPreferences;
    private TextView messageReceivedTextView;
    private EditText typeBoxEditText;
    private final MainActivity mainActivity;

    public BluetoothChatTabFragment(MainActivity main) {
        this.mainActivity = main;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register a local broadcast receiver for incoming messages
        LocalBroadcastManager.getInstance(this.requireContext())
                .registerReceiver(this.mReceiver, new IntentFilter("incomingMessage"));
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Inflate the fragment's UI layout
        View root = inflater.inflate(R.layout.fragment_bluetooth_chat_tab, container, false);

        // Get a reference to the send button
        Button send = root.findViewById(R.id.sendBtn);

        // Get references to the message text view and type box edit text
        this.messageReceivedTextView = root.findViewById(R.id.inputText);
        this.messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());
        this.typeBoxEditText = root.findViewById(R.id.outputText);

        // Get a reference to the shared preferences used for storing messages
        this.sharedPreferences = this.requireActivity()
                .getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        // Set a click listener for the send button
        send.setOnClickListener(view -> {

            if (BluetoothConnectionService.mState != BluetoothConnectionService.STATE_CONNECTED){
                Toast.makeText(getContext(), "Please connect to a device first", Toast.LENGTH_SHORT).show();
            } else {
                String sentText = "" + this.typeBoxEditText.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("message", sharedPreferences
                        .getString("message", "") + '\n' + sentText);
                editor.apply();
                this.messageReceivedTextView.append(sentText + "\n");
                this.typeBoxEditText.setText("");

                byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                BluetoothConnectionService.write(bytes);
            }
        });
        return root;
    }

    /**
     * Returns the text view for displaying received messages.
     *
     * @return the text view for displaying received messages.
     */
    public TextView getMessageReceivedTextView() {
        return this.messageReceivedTextView;
    }

    /**
     * The broadcast receiver for incoming messages.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("receivedMessage");
            messageReceivedTextView.append(text + "\n");
        }
    };

}