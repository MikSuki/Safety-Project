package com.example.y3226.stungun

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.view.View
import android.widget.TextView
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity() {
    // UI
    private lateinit var sendBtn: Button
    private lateinit var connectBtn: Button
    private lateinit var connectStatus: TextView

    // bluetooth device...
    private val MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val REQUEST_ENABLE_BT = 1
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit private var mmDevice: BluetoothDevice
    lateinit private var deviceUUID: UUID
    lateinit private var mConnectedThread: ConnectedThread

    // flag
    private var is_connect = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    fun init() {
        // ------------------------------------------
        // Get UI
        // ------------------------------------------
        sendBtn = findViewById(R.id.sendBtn) as Button
        connectBtn = findViewById(R.id.connectBtn) as Button
        connectStatus = findViewById(R.id.connectStatus) as TextView


        // ------------------------------------------
        // Set bluetooth
        // ------------------------------------------
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Device doesn't support Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Sorry! Your device does not support this feature.",
                    Toast.LENGTH_SHORT).show()
        }
        // Device doesn't open Bluetooth
        if (bluetoothAdapter.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

    }

    // Find the first one from the paired device
    fun pairDevice(v: View) {

        val pairedDevices = bluetoothAdapter.getBondedDevices()

        if (pairedDevices.size > 0) {
            val devices = pairedDevices.toTypedArray()
            val device = devices[0] as BluetoothDevice


            val connect = ConnectThread(device, MY_UUID)
            connect.start()
        }
    }

    private inner class ConnectThread(device: BluetoothDevice, uuid: UUID) : Thread() {
        lateinit private var mmSocket: BluetoothSocket

        init {

            mmDevice = device
            deviceUUID = uuid
        }

        override fun run() {
            lateinit var tmp: BluetoothSocket

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {

            }

            mmSocket = tmp

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect()

            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket.close()
                } catch (e1: IOException) {

                }

            }

            connected(mmSocket)
            // set the status Textview
            runOnUiThread {
                connectStatus.setText("connect")
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }
        }
    }

    private fun connected(mmSocket: BluetoothSocket) {

        is_connect = true
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(mmSocket)
        mConnectedThread.start()
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmOutStream: OutputStream?

        init {
            var tmpOut: OutputStream? = null


            try {
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mmOutStream = tmpOut
        }


        fun write(bytes: ByteArray) {
            try {
                mmOutStream?.write(bytes)
                toast("send")

            } catch (e: IOException) {
            }

        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }

        }
    }


    fun SendMessage(v: View) {
        if (!is_connect) {
            toast("disconnect !")
            return
        }
        val bytes = "send".toByteArray(Charset.defaultCharset())
        mConnectedThread.write(bytes)
    }

    fun toast(str: String) {
        Toast.makeText(this,
                str,
                Toast.LENGTH_SHORT).show()
    }
}
