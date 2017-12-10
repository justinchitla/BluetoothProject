package com.chitlapps.justinchitla.bluetoothapplication;

import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.os.Build;
        import android.os.Bundle;
        import android.support.v7.app.AppCompatActivity;
        import android.util.Log;
        import android.view.View;
        import android.widget.AdapterView;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.ListView;
        import android.widget.TextView;

        import java.nio.charset.Charset;
        import java.util.ArrayList;
        import java.util.UUID;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private static final String TAG = "MainActivity";

    BluetoothAdapter adapter;
    Button discoverableBtn;

    BluetoothConnection connection;

    Button startConnectionBtn;
    Button sendBtn;

    EditText etSend;

    TextView messageTextView;

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    BluetoothDevice mBTDevice;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    public DeviceList mDeviceList;

    ListView lvNewDevices;


    private BroadcastReceiver mBroadcastReceiverForPair = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceList = new DeviceList(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceList);
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiverForBondState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BOND_BONDED.");
                    mBTDevice = mDevice;
                }

                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BOND_BONDING.");
                }

                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BOND_NONE.");
                }
            }
        }
    };



    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiverForPair);
        unregisterReceiver(mBroadcastReceiverForBondState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);
        discoverableBtn = (Button) findViewById(R.id.btnDiscoverable_on_off);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        startConnectionBtn = (Button) findViewById(R.id.btnStartConnection);
        sendBtn = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.editText);
        messageTextView = (TextView) findViewById(R.id.messageTextView);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiverForBondState, filter);

        adapter = BluetoothAdapter.getDefaultAdapter();

        lvNewDevices.setOnItemClickListener(MainActivity.this);


        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

        startConnectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                connection.write(bytes);

                etSend.setText("");
            }
        });

    }

    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }


    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "Initializing RFCOM Connection.");
        connection.startClient(device,uuid);
    }



    public void enableDisableBT(){
        if(adapter == null){
            Log.d(TAG, "Does not have BT capabilities!!!");
        }
        if(!adapter.isEnabled()){
            Log.d(TAG, "enabling BT!");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
        }
        if(adapter.isEnabled()){
            Log.d(TAG, "disabling BT!");
            adapter.disable();
        }

    }


    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "Making device discoverable for 200 seconds!");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
        startActivity(discoverableIntent);
    }

    public void btnDiscover(View view) {
        Log.d(TAG, "Looking for unpaired devices.");

        if(adapter.isDiscovering()){
            adapter.cancelDiscovery();
            Log.d(TAG, "Canceling discovery.");
            adapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiverForPair, discoverDevicesIntent);
        }
        if(!adapter.isDiscovering()){
            adapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiverForPair, discoverDevicesIntent);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        adapter.cancelDiscovery();

        Log.d(TAG, "You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "deviceName = " + deviceName);
        Log.d(TAG, "deviceAddress = " + deviceAddress);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            connection = new BluetoothConnection(MainActivity.this, messageTextView);
        }
    }
}