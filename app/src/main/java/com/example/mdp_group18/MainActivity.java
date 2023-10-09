package com.example.mdp_group18;

import static com.example.mdp_group18.R.color.grassColor;
import static com.example.mdp_group18.R.color.niceGreen;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Activity";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;
    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private GridMap gridMap;
    private int[] curCoord;
    private String direction;
    private TextView robotXCoordText, robotYCoordText, robotDirectionText;
    private ImageButton forwardBtn, backBtn, leftBtn, rightBtn;
    private Button startExplorationBtn, startFastestBtn, resetExplorationBtn;
    private Boolean startExplorationStatus = false;
    private static TextView bluetoothStatus, bluetoothDevice;
    private TextView robotStatus;
    BluetoothAdapter btAdaptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);

        // setting up the adapter
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // add the fragments
        viewPagerAdapter.add(new MapConfigurationTabFragment(MainActivity.this), "Map Configuration");
        viewPagerAdapter.add(new BluetoothChatTabFragment(MainActivity.this), "Manual Chat");

        // Set the adapter
        viewPager.setAdapter(viewPagerAdapter);

        // The Page (fragment) titles will be displayed in the
        // tabLayout hence we need to  set the page viewer
        // we use the setupWithViewPager().
        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        // Creating the gridMap
        this.gridMap = new GridMap(this);
        this.robotXCoordText = findViewById(R.id.coordinatesTextX);
        this.robotYCoordText = findViewById(R.id.coordinatesTextY);
        this.robotDirectionText = findViewById(R.id.directionText);

        this.gridMap = findViewById(R.id.mapView);

        // Set up Controls
        this.forwardBtn = findViewById(R.id.forwardBtn);
        this.rightBtn = findViewById(R.id.rightBtn);
        this.backBtn = findViewById(R.id.backBtn);
        this.leftBtn = findViewById(R.id.leftBtn);

        // Bluetooth Status
        MainActivity.bluetoothStatus = findViewById(R.id.bluetoothStatusText);
        MainActivity.bluetoothDevice = findViewById(R.id.bluetoothConnectedDevices);

        // Robot Status
        this.robotStatus = findViewById(R.id.robotStatus);

        // Set up sharedPreferences
        this.context = getApplicationContext();
        this.sharedPreferences();
        this.editor.putString("message", "");
        this.editor.putString("direction", "None");
        this.editor.putString("connStatus", "Disconnected");
        this.editor.commit();

        // Set up Message Receiver
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        Button bluetoothButton = findViewById(R.id.bluetoothButton);
        bluetoothButton.setOnClickListener(v -> {
            Intent popup = new Intent(MainActivity.this, BluetoothPopup.class);
            this.startActivity(popup);
        });

        forwardBtn.setOnClickListener(view -> {
            moveRobotControl("forward");
        });

        backBtn.setOnClickListener(view -> {
            moveRobotControl("back");
        });

        leftBtn.setOnClickListener(view -> {
            moveRobotControl("left");
        });

        rightBtn.setOnClickListener(view -> {
            moveRobotControl("right");
        });

        // Set Challenge Buttons
        this.startExplorationBtn = findViewById(R.id.startExplorationBtn);
        this.startFastestBtn = findViewById(R.id.startFastestCarBtn);
        this.resetExplorationBtn = findViewById(R.id.resetExplorationBtn);

        startExplorationBtn.setOnClickListener(view -> {

            if (!startExplorationStatus){
                if(this.gridMap.startExplorePrep()){
                    startExplorationBtn.setText("Start Exploration");
                    startExplorationBtn.getBackground().setColorFilter(getColor(niceGreen), PorterDuff.Mode.MULTIPLY);
                    startExplorationStatus=true;
                    resetExplorationBtn.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this,"Please connect to the robot first.",Toast.LENGTH_SHORT).show();
                }
            } else {
                if(this.gridMap.startExplore()){
                    startExplorationBtn.setText("Challenge in progress");
                    startExplorationBtn.setEnabled(false);
                    resetExplorationBtn.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this,"Please connect to the robot first.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        resetExplorationBtn.setOnClickListener(view -> {
            resetExplorationBtn();
            resetExplorationBtn.setVisibility(View.GONE);
        });

        startFastestBtn.setOnClickListener(view -> {
            if(this.gridMap.startFastest()){
                startFastestBtn.setText("Challenge in progress");
                startFastestBtn.setEnabled(false);

            } else {
                Toast.makeText(this,"Please connect to the robot first.",Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void sharedPreferences() {
        this.sharedPreferences = this.getSharedPreferences(this.context);
        this.editor = this.sharedPreferences.edit();
    }
    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }


    public void bluetoothPopup(View view){
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View viewBluetoothPopup = layoutInflater.inflate(R.layout.bluetooth_popup, null);
        PopupWindow popupWindow = new PopupWindow(viewBluetoothPopup, 600, 900, true);
        popupWindow.showAtLocation(view, Gravity.CENTER,0,0);
        popupWindow.setAnimationStyle(R.style.popup_window_animation_phone);
    }


    public GridMap getGridMap() {
        return this.gridMap;
    }

    public void refreshDirection(String direction) {
        this.gridMap.setRobotDirection(direction);
        this.robotDirectionText.setText(this.sharedPreferences.getString("direction", ""));
    }

    private void moveRobotControl(String control){

        String controlText;
        byte[] bytes;

        if (this.gridMap.getCanDrawRobot()) {
            this.curCoord = this.gridMap.getCurCoord();
            this.direction = this.gridMap.getRobotDirection();


            switch (control){
                case "forward":

                    switch (this.direction) {
                        case "N":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0], this.curCoord[1] + 1}, 0, control);
                            break;
                        case "W":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] - 1, this.curCoord[1]}, 0, control);
                            break;
                        case "S":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0], this.curCoord[1] - 1}, 0, control);
                            break;
                        case "E":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] + 1, this.curCoord[1]}, 0, control);
                            break;
                    }
                    break;

                case "back":

                    switch (this.direction) {
                        case "N":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0], this.curCoord[1] - 1}, 0, control);
                            break;
                        case "W":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] + 1, this.curCoord[1]}, 0, control);
                            break;
                        case "S":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0], this.curCoord[1] + 1}, 0, control);
                            break;
                        case "E":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] - 1, this.curCoord[1]}, 0, control);
                            break;
                    }
                    break;

                case "left":

                    switch (this.direction) {
                        case "N":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] - 4, this.curCoord[1] + 2}, 90, control);
                            break;
                        case "W":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] - 2, this.curCoord[1] - 4}, 90, control);
                            break;
                        case "S":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] + 4, this.curCoord[1] - 2}, 90, control);
                            break;
                        case "E":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] + 2, this.curCoord[1] + 4}, 90, control);
                            break;
                    }
                    break;

                case "right":

                    switch (this.direction) {
                        case "N":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] + 4, this.curCoord[1] + 2}, -90, control);
                            break;
                        case "W":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] - 2, this.curCoord[1] + 4}, -90, control);
                            break;
                        case "S":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] - 4, this.curCoord[1] - 2}, -90, control);
                            break;
                        case "E":
                            this.gridMap.moveRobot(new int[]{this.curCoord[0] + 2, this.curCoord[1] - 4}, -90, control);
                            break;
                    }
                    break;
            }

            // refreshes the UI displayed coordinate of robot
            this.refreshCoordinate();
        }
        else
            Toast.makeText(this, "Please place robot on map to begin",Toast.LENGTH_SHORT).show();
    }

    public void refreshCoordinate() {

        String xCoord = this.getString(R.string.xCoordinate, String.valueOf(this.gridMap.getCurCoord()[0]));
        String yCoord = this.getString(R.string.yCoordinate, String.valueOf(this.gridMap.getCurCoord()[1]));

        this.robotXCoordText.setText(xCoord);
        this.robotYCoordText.setText(yCoord);
        this.robotDirectionText.setText(this.sharedPreferences.getString("direction", ""));
    }

    public static TextView getBluetoothStatus() {
        return bluetoothStatus;
    }

    public static TextView getConnectedDevice() {
        return bluetoothDevice;
    }

    public void resetExplorationBtn(){
        this.startExplorationStatus = false;
        this.startExplorationBtn.setText("Exploration Setup");
        this.startExplorationBtn.setEnabled(true);
        this.startExplorationBtn.getBackground().clearColorFilter();
    }

    /**
     * Handles message sent from RPI
     * Message format:
     *
     * For updating image ID during image rec:
     * TARGET,[obstacle id],[image id]
     *  ex: TARGET,3,7 for obstacle OB3 === image id 7
     *
     * For updating robot coordinates/ direction:
     * ROBOT,[x-coord],[y-coord],[direction]
     *   ex 1: ROBOT,4,6,N for moving robot to [4,6], facing N
     *   ex 2: ROBOT,6,6,E for moving robot to [6,6], facing E
     *
     * For updating status of robot:
     * STATUS,[new status]
     *
     * For signaling Android that task is completed
     * ENDED
     */
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            System.out.println("debug" + message);
            if (message.contains("TARGET")) {
                String[] cmd = message.split(",");

                Log.d(TAG, "Receiving Message: Target - ObstacleID = " + cmd[1] + ", ImageID = " + cmd[2]);

                int imageID = Integer.parseInt(cmd[2].trim());
                if (11 <= imageID && imageID <= 40){
                    gridMap.updateImageID(cmd[1], cmd[2].trim());
                } else {
                    Toast.makeText(context,"Invalid Image ID = " + imageID, Toast.LENGTH_SHORT).show();

                }

            } else if (message.contains("ROBOT")) {
                String[] cmd = message.split(",");
                int xPos = (int) Float.parseFloat(cmd[1]);
                int yPos = (int) Float.parseFloat(cmd[2]);
                String direction = cmd[3].trim();
                switch (direction) {
                    case "N":
                        GridMap.robotBearing = 90;
                        break;
                    case "W":
                        GridMap.robotBearing = 180;
                        break;
                    case "S":
                        GridMap.robotBearing = 270;
                        break;
                    case "E":
                        GridMap.robotBearing = 0;
                        break;
                }
                gridMap.setCurCoord(xPos,yPos,direction);
            } else if (message.contains("STATUS")){
                String[] cmd = message.split(",");
                String updateStatus =  cmd[1].trim();
                robotStatus.setText(updateStatus);
            } else if (message.contains("EXPLORE")){

                resetExplorationBtn();

            } else if (message.contains("FASTEST")){
                startFastestBtn.setEnabled(true);
                startFastestBtn.setText("Start Fastest Car");
            }
        }
    };

}

