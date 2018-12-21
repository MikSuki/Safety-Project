package com.example.y3226.safety

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.pm.PackageManager
import android.widget.Toast
import android.telephony.SmsManager
import android.view.View
import android.widget.EditText
import android.widget.TextView
import java.io.IOException
import java.io.InputStream
import java.util.*


class MainActivity : AppCompatActivity() {
    // UI
    private lateinit var phoneNumber: EditText
    private lateinit var messageBox: EditText
    private lateinit var connectBtn: Button
    private lateinit var connectStatus: TextView

    // bluetooth device...
    private val MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val REQUEST_ENABLE_BT = 1
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit private var mConnectedThread: ConnectedThread


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    fun init() {

        messageBox = findViewById(R.id.MessageBox) as EditText
        phoneNumber = findViewById(R.id.phoneNumber) as EditText
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

    fun sendSMS() {
        val messageToSend = messageBox.text.toString()
        val number = phoneNumber.text.toString()

        if (number.length == 10 && messageToSend.length > 0) {

            // SdkVersion > 23 need to check permission before use
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf<String>(Manifest.permission.SEND_SMS)
                    requestPermissions(permissions, 1)
                }
            }

            SmsManager.getDefault().sendTextMessage(number, null, messageToSend, null, null)

            toast("send to " + phoneNumber.text + " over ～")
        } else if(number.length < 10)
            toast("phone number format error !")
        else
            toast("you need to write something !")

    }


    private fun connected(mmSocket: BluetoothSocket) {

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(mmSocket)
        mConnectedThread.start()
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?

        init {
            var tmpIn: InputStream? = null


            try {
                tmpIn = mmSocket.inputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mmInStream = tmpIn
        }

        override fun run() {
            val buffer = ByteArray(1024)  // buffer store for the stream

            var bytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream!!.read(buffer)

                    runOnUiThread {
                        sendSMS()
                    }

                } catch (e: IOException) {

                    break
                }
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


    private inner class AcceptThread : Thread() {

        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null

            // Create a new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("appname", MY_UUID)

            } catch (e: IOException) {

            }

            mmServerSocket = tmp
        }

        override fun run() {

            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                socket?.also {
                    connected(socket)
                    // set the status Textview
                    runOnUiThread {
                        connectStatus.setText("connect")
                    }
                    shouldLoop = false
                    mmServerSocket?.close()
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
            }
        }
    }

    fun Start_Server(view: View) {

        val accept = AcceptThread()
        accept.start()
    }

    fun toast(str: String) {
        Toast.makeText(this,
                str,
                Toast.LENGTH_SHORT).show()
    }
}
