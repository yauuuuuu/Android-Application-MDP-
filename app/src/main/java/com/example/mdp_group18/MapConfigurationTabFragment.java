package com.example.mdp_group18;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MapConfigurationTabFragment extends Fragment {

    private static final String TAG = "MapFragment";
    SharedPreferences mapPref;
    private static SharedPreferences.Editor editor;

    ImageButton mapResetBtn;
    ToggleButton setRobotBtn, setObstacleBtn;
    GridMap gridMap;
    Switch dragSwitch, updateObstacleSwitch;
    Button updateRobotDirectionBtn;
    static boolean dragStatus;
    static boolean changeObstacleStatus;
    private MainActivity mainActivity;
    public MapConfigurationTabFragment(MainActivity main) {
        this.mainActivity = main;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map_configuration_tab, container, false);


        gridMap = this.mainActivity.getGridMap();

        final DirectionFragment directionFragment = new DirectionFragment();

        mapResetBtn = root.findViewById(R.id.mapResetBtn);
        setRobotBtn = root.findViewById(R.id.setRobotBtn);
        updateRobotDirectionBtn = root.findViewById(R.id.updateRobotDirectionBtn);
        setObstacleBtn = root.findViewById(R.id.setObstacleBtn);
        updateObstacleSwitch = root.findViewById(R.id.updateObstacleSwitch);
        dragSwitch = root.findViewById(R.id.dragSwitch);


        mapResetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Reseting map...");
                gridMap.resetMap(true);
            }
        });


        // switch for dragging
        dragSwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                showToast("Dragging is " + (isChecked ? "on" : "off"));
                dragStatus = isChecked;
                if (dragStatus) {
                    gridMap.setSetObstacleStatus(false);
                    if (setRobotBtn.isChecked()) {
                        setRobotBtn.toggle();
                    }
                    if (setObstacleBtn.isChecked()) {
                        setObstacleBtn.toggle();
                    }
                    updateObstacleSwitch.setChecked(false);
                }
            }
        });


        // switch for changing obstacle
        updateObstacleSwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                showToast("Changing Obstacle is " + (isChecked ? "on" : "off"));
                changeObstacleStatus = isChecked;
                if (changeObstacleStatus) {
                    gridMap.setSetObstacleStatus(false);
                    if (setRobotBtn.isChecked()) {
                        setRobotBtn.toggle();
                    }
                    if (setObstacleBtn.isChecked()) {
                        setObstacleBtn.toggle();
                    }
                    dragSwitch.setChecked(false);
                }
            }
        });

        setRobotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setStartPointToggleBtn");
                if (setRobotBtn.getText().equals("SET START POINT")) {
                    gridMap.setCanDrawRobot(false);
                    gridMap.setStartCoordStatus(false);
                    gridMap.toggleCheckedBtn("setRobotBtn");
                }
                else if (setRobotBtn.getText().equals("CANCEL")) {
                    gridMap.setStartCoordStatus(true);
                    gridMap.setCanDrawRobot(true);
                    gridMap.toggleCheckedBtn("setRobotBtn");
                }
                setObstacleBtn.setChecked(false);
                dragSwitch.setChecked(false);
            }
        });

        updateRobotDirectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked directionChangeImageBtn");
                directionFragment.show(getActivity().getFragmentManager(),
                        "Direction Fragment");
                showLog("Exiting directionChangeImageBtn");
            }
        });

        setObstacleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked obstacleImageBtn");

                if (!gridMap.getSetObstacleStatus()) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                }
                else {
                    int numObstacles = gridMap.getObstacleCoord().size();
                    showToast(numObstacles + " obstacles plotted");
                    gridMap.setSetObstacleStatus(false);
                }

                updateObstacleSwitch.setChecked(false);
                dragSwitch.setChecked(false);
                showLog("obstacle status = " + gridMap.getSetObstacleStatus());
                showLog("Exiting obstacleImageBtn");
            }
        });



        return root;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}