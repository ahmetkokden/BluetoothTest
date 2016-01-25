package kr.ac.kookmin.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;



    // My Laptop Address : 80:86:F2:67:1E:8D

public class MainActivity extends AppCompatActivity {


    /*
        Activity Tag String
     */
    private static final String TAG = "MainActivity";

    /*
        Intent Request Codes
     */
    private static final int REQUEST_ENABLE_BT = 1;

    /*
        Bluetooth Adapter, Device and Socket
     */
    public BluetoothAdapter mBluetoothAdapter = null;
    public BluetoothDevice mBluetoothDevice = null;
    public BluetoothSocket mmSocket = null;

    /*
        List and Adapter for Found Bluetooth Devices
     */

    /*
    ArrayList deviceStrs = new ArrayList();
    final ArrayList devices = new ArrayList();
    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
    ArrayAdapter mArrayAdapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice);
    */
    /*
        Device Address String
     */
    public String deviceAddress = null;

    /*
        Input and Output Stream
     */
    public InputStream mmInputStream = null;
    public OutputStream mmOutputStream = null;

    /*
        Stream Buffer
     */
    public byte[] buffer = new byte[2048];
    int bytes;

    /*
        Buffer Handling Thread
     */
    /*
    Thread connectedThread = new Thread(new Runnable() {

        String temp = null;
        @Override
        public void run() {
            while(true) {
                try {
                    bytes = mInputStream.read(buffer);
                    temp = buffer.toString().substring(bytes);
                    Log.d(TAG, temp);
                } catch(IOException e) {
                    Log.e(TAG, "Connected Thread Error");
                }
            }
        }
    });
    */

    Thread workerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.e(TAG, "Bluetooth Not Supported");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device : pairedDevices)
        {
            Log.d("Device","This is the device address : " + device.getAddress());
            Log.d("Device", "This is the device name : " + device.getName());
            deviceAddress = device.getAddress();
            mBluetoothDevice = device;
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        try {
            mmSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            new EchoOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            new LineFeedOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            new TimeoutCommand(125).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            new AmbientAirTemperatureCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
        } catch (Exception e) {
            //  Handle Errors
        }





        final Handler handler = new Handler();

        workerThread = new Thread(new Runnable()
        {
            public void run()
            {

                while(true)
                {
                    //Do work
                    try {
                        new EchoOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        new LineFeedOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        new TimeoutCommand(125).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        new SelectProtocolCommand(ObdProtocols.AUTO).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        new AmbientAirTemperatureCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    } catch (Exception e) {
                        //  Handle Errors
                    }

                    RPMCommand engineRpmCommand = new RPMCommand();
                    SpeedCommand speedCommand = new SpeedCommand();
                    while (!Thread.currentThread().isInterrupted())
                    {
                        try {
                            engineRpmCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                            speedCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        }
                        catch (Exception e) {
                        }
                        // TODO handle commands result
                        Log.d("Tag", "RPM: " + engineRpmCommand.getFormattedResult());
                        Log.d("Tag", "Speed: " + speedCommand.getFormattedResult());
                    }

                    /*try {
                        mmOutputStream.write(0x01);
                        mmOutputStream.write(0x05);
                        mmOutputStream.write("\r".getBytes());
                        mmOutputStream.flush();
                        SystemClock.sleep(1000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    int bytesAvailable = 0;
                    try {
                        bytesAvailable = mmInputStream.available();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(bytesAvailable > 0)
                    {
                        byte[] packetBytes = new byte[bytesAvailable];
                        try {
                            mmInputStream.read(packetBytes);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d("DataRead", packetBytes.toString());
                        Log.d("DataHex", bytesToHex(packetBytes));
                    }*/
                }
            }
        });
        workerThread.start();


        /*
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        }
        */

        /*
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }


        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress = (String) devices.get(position);
            }
        });

        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
        */

        /*
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            socket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mInputStream = socket.getInputStream();
            mOutputStream = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "I/O Stream Assign Error");
            e.printStackTrace();
        }
        */
        //connectedThread.start();
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
