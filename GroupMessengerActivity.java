// ISIS algorithm is implemented to achieve total ordering.

package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();

    static final String REMOTE_PORT[]= {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    private String fail_port="";
    //static int proposed_seq_no;
    static int seq_no_proposal = 0;
    private static String KEY_FIELD = "key";
    private static String VALUE_FIELD = "value";
    PriorityQueue<MessageInfo> pq;
    PriorityQueue<MessageInfo> pq_temp1;
    //    PriorityQueue<MessageInfo> pq_temp2;
    String myPort;
    //    boolean alive [];
//    Alive alive_arr[];
    static int content_provider_seq_no=0;
    List <String> failed_nodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        // My code
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        // PA2B code
        pq = new PriorityQueue<MessageInfo>();
        pq_temp1 = new PriorityQueue<MessageInfo>();
//        alive_arr = new Alive[5];
//        pq_temp2 = new PriorityQueue<MessageInfo>();
//        for (int i = 0; i < 5; i++)
//            alive[i] = true;
        failed_nodes = new ArrayList<String>();
        //failed_nodes.add("11116");
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        // My code ends

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        // My code
        final EditText editText = (EditText) findViewById(R.id.editText1);
        // https://developer.android.com/reference/android/widget/Button
        final Button send = (Button)findViewById(R.id.button4);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg =editText.getText().toString()+"\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
//            return false;
        });
        // My code ends
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    // Reference: https://stackoverflow.com/questions/29872664/add-key-and-value-into-an-priority-queue-and-sort-by-key-in-java
    class MessageInfo implements Comparable<MessageInfo> {
        private float seq_no;
        private String message, client_port;
        private boolean fixed;

        MessageInfo(float seq_no, String message, boolean fixed, String client_port) {
            this.seq_no = seq_no;
            this.message = message;
            this.fixed = fixed;
            this.client_port = client_port;
        }
        @Override
        public int compareTo(MessageInfo other_message) {
            Float my_seq =new Float(this.seq_no);
            Float other_seq = new Float(other_message.seq_no);
            int result = my_seq.compareTo(other_seq);
            return my_seq.compareTo(other_seq);
        }
    }

    //    private class Alive{
//        String client_port;
//        boolean is_alive;
//
//        Alive(String client_port) {
//            this.client_port = client_port;
//            this.is_alive = true;
//        }
//    }
    // My code
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                while (true) {
                    Socket socket_s = serverSocket.accept();
//                    //socket_s.setSoTimeout(1500);             // Reference: https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
                    DataInputStream din_server = new DataInputStream(socket_s.getInputStream());
                    try {
                        try {
                            Log.e(TAG, "Priority queue empty: " + Boolean.toString(pq.isEmpty()) + ", failed nodes contain pq front: " + Boolean.toString(failed_nodes.contains(pq.peek().client_port)) + " , pq peek fixed: " + Boolean.toString(pq.peek().fixed)+"Failed nodes queue:"+Arrays.toString(failed_nodes.toArray()));
                        }
                        catch (NullPointerException e){}
                        while(!pq.isEmpty() && fail_port.equals(pq.peek().client_port) ){//&& (pq.peek().fixed == false)) {
                            Log.e(TAG,"Exception handling: Removing "+pq.peek().seq_no+", Client: "+pq.peek().client_port+", "+pq.peek().message+" fixed = "+pq.peek().fixed+" from priority queue");
                            pq.remove();
                        }
//                        Log.e(TAG, "Priority queue front after exception handling: "+pq.peek().seq_no+", "+pq.peek().message+", fixed = "+pq.peek().fixed);
//                        Log.e(TAG, "Server side: Sequence number going to be proposed next is: "+seq_no_proposal);
                        socket_s.setSoTimeout(1000);
                        String strings = din_server.readUTF();
                        // Sequence number request
                        if(strings.contains("Sequence number request")) {
                            strings = strings.replace("Sequence number request","");
                            String port_no_str = strings.substring(0,5);
                            strings = strings.replaceFirst(port_no_str,"");
                            String client_port = strings.substring(0,5);
                            String message = strings.replace(client_port, "");
                            // Decide sequence number to propose
//                            MessageInfo temp;
//                            while(pq.size()!=1) {
//                                temp = pq.remove();
//                            }
//                            while(pq_temp1.size()!=0) {
//                                temp = pq_temp1.remove();
//                                pq.add(temp);
//                            }
                            String proposed_seq_no_str = Integer.toString(seq_no_proposal);
                            String seq_port_no_str = proposed_seq_no_str+"."+port_no_str;
                            DataOutputStream dout_server = new DataOutputStream(socket_s.getOutputStream());
                            dout_server.writeUTF("Message received"+seq_port_no_str);

                            float seq_port_no = Float.parseFloat(seq_port_no_str);
                            //pq.add(seq_port_no);
                            MessageInfo msg_temp = new MessageInfo(seq_port_no,message,false, client_port);
                            pq.add(msg_temp);
                            Log.e(TAG, "Server side: Adding temp msg into pq with sq no: "+Float.toString(msg_temp.seq_no)+" , message: "+msg_temp.message+", client port: "+msg_temp.client_port);
                            Log.e(TAG, "Server side: while adding, front msg is: "+Float.toString(pq.peek().seq_no)+" , message: "+pq.peek().message);
                            seq_no_proposal++;
                        }
                        else if (strings.contains("Final message")) {
                            /********************************* Separating msg, port, seq no *********************************/
                            String client_port = strings.substring(0,5);
                            strings = strings.replaceFirst(client_port,"");
                            int substr_index = strings.indexOf("Final message");
                            String final_seq_no_str = strings.substring(0,substr_index);
                            float final_seq_no = Float.parseFloat(final_seq_no_str);
                            if((int) final_seq_no > seq_no_proposal) {
                                Log.e(TAG,"Server side: Replacing seq_no_proposal from "+seq_no_proposal+" to "+(int) final_seq_no+1);
                                seq_no_proposal = (int) final_seq_no+1;
                            }
                            strings = strings.replace("Final message","");
                            String message = strings.replace(final_seq_no_str,"");
                            // Put final message along with final seq no in the queue
                            MessageInfo msg_final = new MessageInfo(final_seq_no,message,true, client_port);
                            MessageInfo temp;
                            // Reference: https://stackoverflow.com/questions/12719066/priority-queue-remove-complexity-time
                            /*********************************** Replacing old sequence number with agreed one ***************************************/
                            while(pq.size()!=0) {
                                temp = pq.remove();
                                if (temp.message.equals(message) && temp.client_port.equals(client_port)) {
                                    Log.e(TAG, "Server side: Replacing old seq no: "+Float.toString(temp.seq_no)+" by final: "+Float.toString(msg_final.seq_no)+" , message: "+msg_final.message);
                                    break;
                                }
                                else
                                    pq_temp1.add(temp);
                            }
                            while(pq_temp1.size()!=0) {
                                temp = pq_temp1.remove();
                                pq.add(temp);
                            }

                            pq.add(msg_final);
                            Log.e(TAG, "Server side: After adding final seq no, Seq no at queue front: "+Float.toString(pq.peek().seq_no)+" ,message: "+pq.peek().message);

                            /*************** If front of priority queue is having agreed seq no, deliver that message *****************/
                            //MessageInfo temp2;
                            while(pq.size()!=0) {
                                temp = pq.remove();
                                if (temp.fixed) {
//                                    Log.e(TAG, "Server side: While delivering Seq no at queue front: "+Float.toString(temp.seq_no)+" with status: "+temp.fixed+" , message: "+temp.message);
                                    publishProgress(Float.toString(temp.seq_no)+"separator"+temp.message+Boolean.toString(temp.fixed));
                                }
                                else {
                                    pq.add(temp);
                                    break;
                                }
                            }
//                            while(pq_temp2.size()!=0) {
//                                temp = pq_temp2.remove();
//                                pq.add(temp);
//                            }
                            DataOutputStream dout_server = new DataOutputStream(socket_s.getOutputStream());
                            dout_server.writeUTF("Message received");
                        }
                    }
                    catch (SocketTimeoutException e)    // Reference: https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
                    {
                        Log.e(TAG, "Socket timeout exception");
                    }
//                    socket_s.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {
            String strReceived = strings[0].trim();
            if(strReceived.contains("true")) {
                strReceived = strReceived.replace("true","");
                int separator = strReceived.indexOf("separator");
                String final_seq_no_str = strReceived.substring(0, separator);
                String message = strReceived.replace(final_seq_no_str, "").replaceFirst("separator","");
                TextView textView = (TextView) findViewById((R.id.textView1));
                textView.append(message + "\t\n");

                String filename = final_seq_no_str;
                String string = message;

                try {
                    TextView tv = (TextView) findViewById(R.id.textView1);
                    final TextView mTextView = tv;
                    final ContentResolver mContentResolver = getContentResolver();
                    final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                    ContentValues cv = new ContentValues();
                    //final ContentValues mContentValues = initTestValues(final_seq_no_str, message);
                    final ContentValues mContentValues = initTestValues(Integer.toString(content_provider_seq_no), message);
                    mContentResolver.insert(mUri, mContentValues);
                    Log.e(TAG, "Server side: While delivering Seq no at queue front: "+final_seq_no_str+" , message: "+message+" key:"+Integer.toString(content_provider_seq_no));
                    content_provider_seq_no++;
                } catch (Exception e) {
                    Log.e(TAG, "File write failed");
                }
            }
            return;
        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private ContentValues initTestValues(String seq_no_str, String string) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_FIELD,  seq_no_str);
        cv.put(VALUE_FIELD, string);
        return cv;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            List<Float> proposed_seq_no_list = new ArrayList<Float>();
            // ask for sequence number
            for(int i = 0; i < 5; i++) {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[i]));
                    // pass the port no so that it along with seq no gets added in servers priority queue
//                    String self_port = Integer.toString(socket.getLocalPort());
//                    String self_address = socket.getInetAddress();

                    int server_port = socket.getPort();
                    if(server_port == 11120)
                        server_port = 11121;
                    String server_port_str = Integer.toString(server_port);
                    String mp = myPort;
                    String msgToSend = "Sequence number request"+ server_port_str+myPort+msgs[0];
//                    String msgToSend2 = ;
//                    String msgToSend = msgToSend1+msgToSend2;

                    DataOutputStream dout_client = new DataOutputStream(socket.getOutputStream());
                    Log.e(TAG, "Client side: Sending sequence number request for message: "+msgs[0].replace("\n","")+" to emulator "+Integer.toString(i));
                    dout_client.writeUTF(msgToSend);
//                    //socket.setSoTimeout(1500);
                    DataInputStream din_client = new DataInputStream(socket.getInputStream());
                    try {
                        socket.setSoTimeout(1000);
                        String str_received = din_client.readUTF();
                        if (str_received.contains("Message received")) {
                            String proposed_seq_no_str = str_received.replace("Message received","");
                            Log.e(TAG, "Client side: Proposed number "+proposed_seq_no_str+ " received for message: "+msgs[0].replace("\n","")+" from emulator "+Integer.toString(i));
                            //String seq_port_no_str = proposed_seq_no_str+"."+sender_port_str;
                            float proposed_seq_no = Float.parseFloat(proposed_seq_no_str);
                            proposed_seq_no_list.add(proposed_seq_no);
                            dout_client.flush();
                            socket.close();
                        }
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "Socket timeout exception");
                        String failed_server = REMOTE_PORT[i];
                        fail_port=REMOTE_PORT[i];
                        if(fail_port == "11120")
                            fail_port = "11121";
                        if (!failed_nodes.contains(failed_server))
                            Log.e(TAG, "Client side: ClientTask socket IOException: Adding server "+failed_server+" to failed nodes list");
                        failed_nodes.add(failed_server);
                        Log.e(TAG,"Client side: Exception handling: Failed node list: "+Arrays.toString(failed_nodes.toArray()));
                    }
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "Client side: ClientTask socket IOException while requesting seq no for message: "+msgs[0].replace("\n","")+" to emulator "+Integer.toString(i));
//                    Log.e(TAG, "CHECK:"+e.getMessage());
                    String failed_server = REMOTE_PORT[i];
                    fail_port=REMOTE_PORT[i];
                    if(fail_port == "11120")
                        fail_port = "11121";
                    if (!failed_nodes.contains(failed_server))
                        Log.e(TAG, "Client side: ClientTask socket IOException: Adding server "+failed_server+" to failed nodes list");
                        failed_nodes.add(failed_server);
                    Log.e(TAG,"Client side: Exception handling: Failed node list: "+Arrays.toString(failed_nodes.toArray()));
                }
            }
            // Choose the highest proposed no as sequence number
            float final_seq_no = Collections.max(proposed_seq_no_list);
//            Log.e(TAG, "Client side: At client side, for message "+msgs[0].replace("\n","")+" from the proposed sequence numbers, "+ Arrays.toString(proposed_seq_no_list.toArray())+" Max finalized is: "+Float.toString(final_seq_no));

            //send the message
            for(int i = 0; i < 5; i++) {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[i]));
                    String msgToSend = myPort+Float.toString(final_seq_no)+"Final message"+msgs[0];

                    DataOutputStream dout_client = new DataOutputStream(socket.getOutputStream());
                    dout_client.writeUTF(msgToSend);
//                    //socket.setSoTimeout(1500);
                    Log.e(TAG, "Client side: sending final message "+msgs[0].replace("\n","")+" with seq no "+Float.toString(final_seq_no)+" to emulator "+Integer.toString(i));
                    DataInputStream din_client = new DataInputStream(socket.getInputStream());
                    try {
                        socket.setSoTimeout(1000);
                        if (din_client.readUTF().equals("Message received")) {
                            dout_client.flush();
                            socket.close();
                        }
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "Socket timeout exception");
                        String failed_server = REMOTE_PORT[i];
                        fail_port=REMOTE_PORT[i];
                        if(fail_port == "11120")
                            fail_port = "11121";
                        if (!failed_nodes.contains(failed_server))
                            Log.e(TAG, "Client side: ClientTask socket IOException: Adding server "+failed_server+" to failed nodes list");
                        failed_nodes.add(failed_server);
                        Log.e(TAG,"Client side: Exception handling: Failed node list: "+Arrays.toString(failed_nodes.toArray()));
                    }
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "Client side: ClientTask socket IOException in final message with seq no: "+Float.toString(final_seq_no)+" message: "+msgs[0].replace("\n","")+" to emulator "+Integer.toString(i));
//                    Log.e(TAG, "CHECK:"+e.getMessage());
                    String failed_server = REMOTE_PORT[i];
                    fail_port=REMOTE_PORT[i];
                    if(fail_port == "11120")
                        fail_port = "11121";
                    if (!failed_nodes.contains(failed_server))
                        Log.e(TAG, "Client side: ClientTask socket IOException: Adding server "+failed_server+" to failed nodes list");
                    failed_nodes.add(failed_server);
                    Log.e(TAG,"Client side: Exception handling: Failed node list: "+Arrays.toString(failed_nodes.toArray()));
                }
            }
            return null;
        }
    }
    // My code ends
}