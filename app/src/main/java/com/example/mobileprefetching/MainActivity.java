package com.example.mobileprefetching;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.view.View;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    final Context context = this;
    final String TAG =  "MapClient";
    final String HOSTNAME = "marvin.cse.buffalo.edu";
    final int PORT = 1234;
    final int IMAGE_REQUEST_CODE = 50;
    final int XYZ_REQUEST_CODE = 25;
    int last_id = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView infoTextView = findViewById(R.id.infoText);
        infoTextView.setText("Click on an image or enter coordinates.");

        GridView gridview = findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));

        final ImageView selectedImageView = findViewById(R.id.imageView);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Log.d("clciked","image");
                String filename = "imgs" + (position + 1) + ".jpg";
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                AssetManager assetManager = context.getAssets();
                try {
                    InputStream ims = assetManager.open(filename);

                    Drawable d = Drawable.createFromStream(ims, null);
                    selectedImageView.setImageDrawable(d);
                    InputStream ims2 = assetManager.open(filename);
                    byte[] buffer = new byte[1024];
                    int len;

                    // read bytes from the input stream and store them in buffer
                    while ((len = ims2.read(buffer)) != -1) {
                        // write bytes from the buffer into output stream
                        os.write(buffer, 0, len);
                    }

                    new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, os);
                } catch (Exception e) {}


               // new sendImageToServerTask(assetManager, infoTextView).execute(filename);
            }
        });



        }

    class sendCoordsToServerTask extends AsyncTask<Float, Void, String> {
        /***
         * Send image to server and receive xyz coordinates in response.
         */

        TextView infoTextView;
        AssetManager assetManager;
        ImageView selectedImageView;

        sendCoordsToServerTask(TextView infoTextView, AssetManager assetManager, ImageView selectedImageView) {
            this.infoTextView = infoTextView;
            this.assetManager = assetManager;
            this.selectedImageView = selectedImageView;
        }

        @Override
        protected String doInBackground(Float...coords) {
            try {
                SocketChannel sock = SocketChannel.open();
                sock.connect(new InetSocketAddress(HOSTNAME, PORT));

                Log.d(TAG, "Connected to server.");

                ByteBuffer buf = ByteBuffer.allocate(24);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.clear();

                Log.d(TAG, "Writing to server.");

                // First, send a byte signifying what kind of request this is.
                buf.putInt(XYZ_REQUEST_CODE);
                // Pad buffer with int
                buf.putInt(10);

                buf.putFloat(coords[0]);
                buf.putFloat(coords[1]);
                buf.putFloat(coords[2]);
                buf.putFloat(coords[3]);

                buf.flip();
                sock.write(buf);

                Log.d(TAG, "Finished writing to server.");



//                while (sock.isConnected()) {
//                    ByteBuffer readBuf = ByteBuffer.allo
//                    int bytesRead = sock.read()
//
//                    byte[] byteArray = new byte[8192];
//                    String filename = "img_" + last_id + ".jpg";
//
//                    FileOutputStream fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//
//                    int count = input.read(byteArray);
//                    while (count > 0) {
//                        fileOutputStream.write(byteArray, 0, count);
//                        count = input.read(byteArray);
//                    }
//
//                    fileOutputStream.flush();
//                    fileOutputStream.close();
//
//                    String result = filename;
//                    return result;
//                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending image to server!", e);
                return null;
            }

            return null;
        }

        protected void onPostExecute(String filename) {
            if (filename == null) {
                infoTextView.setText("Error occurred, check logs.");
            } else {
                infoTextView.setText("Image found and updated on left.");
                try {
                    InputStream ims = assetManager.open(filename);
                    Drawable d = Drawable.createFromStream(ims, null);
                    selectedImageView.setImageDrawable(d);
                } catch (IOException e) {
                    infoTextView.setText("Image downloaded but couldn't be opened.");
                }
            }
        }

    }

    private class ClientTask extends AsyncTask<ByteArrayOutputStream, Void, String> {

        private static final int PORT = 5002;
        private static final String IP = "10.84.110.91";
        @Override
        protected String doInBackground(ByteArrayOutputStream... byteArrayOutputStreams) {
            String detected="Processing...";
            Socket socket = null;
            try {
                Log.d("MobilPrefetching","inside client task");
                ByteArrayOutputStream msgToSend = byteArrayOutputStreams[0];
                byte[] byteArray = msgToSend.toByteArray();
                int size = byteArray.length;

                Log.d("imageSize",new Integer(size).toString());

                socket = new Socket(InetAddress.getByName(IP), PORT);
                OutputStream outputStream = socket.getOutputStream();

                DataOutputStream dos = new DataOutputStream(outputStream);
                dos.writeInt(size);
                //int offset = 0;
                //while(offset<size) {
                dos.write(byteArray, 0, size);
                //}
                dos.flush();
                ObjectInputStream objReader= new ObjectInputStream(socket.getInputStream());
                List<Double> output = (ArrayList<Double>)objReader.readObject();

                // detected=(String) objReader.readObject();
                //Log.d("readline",detected);
                // socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {

                Log.e(TAG, "ClientTask socket IOException");
            }
            catch (Exception e){
                Log.e(TAG, "general msg"+e.getMessage());
            }
            finally {
                if(socket!=null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return detected;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

        }
    }

    class sendImageToServerTask extends AsyncTask<String, Void, String> {
        /***
         * Send image to server and receive xyz coordinates in response.
         */

        AssetManager assetManager;
        TextView infoTextView;

        sendImageToServerTask(AssetManager assetManager, TextView infoTextView) {
            this.assetManager = assetManager;
            this.infoTextView = infoTextView;
        }

        @Override
        protected String doInBackground(String...filename) {
            try {
                SocketChannel sock = SocketChannel.open();
                sock.connect(new InetSocketAddress(HOSTNAME, PORT));

                Log.d(TAG, "Connected to server.");

                ByteBuffer buf = ByteBuffer.allocate(24);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.clear();

                Log.d(TAG, "Writing to server.");

                // First, send a byte signifying what kind of request this is.
                buf.putInt(IMAGE_REQUEST_CODE);
                // Pad buffer with int
                buf.putInt(10);

                // Create bytearray to output image
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                BitmapFactory.decodeStream(assetManager.open(filename[0])).compress(
                        Bitmap.CompressFormat.PNG, 100, stream
                );
                byte[] imageByteArray = stream.toByteArray();

                // Then, send size of file.
                buf.putLong(imageByteArray.length);
                Log.d(TAG, "Image data length = " + imageByteArray.length);

                buf.flip();
                sock.write(buf);

                // Then, send the image
                ByteBuffer imageBuf = ByteBuffer.wrap(imageByteArray);
                buf.flip();
                sock.write(imageBuf);


                ByteBuffer responseBuf = ByteBuffer.allocate(12);


                sock.read(responseBuf);
                String x = String.valueOf(responseBuf.getFloat());
                String y = String.valueOf(responseBuf.getFloat());
                String z = String.valueOf(responseBuf.getFloat());

                String update = "Localization complete. (" + x + ", " + y + ", " + z + ")";
                return update;
            } catch (IOException e) {
                Log.e(TAG, "Error sending image to server!", e);
                return null;
            }
        }

        protected void onPostExecute(String update) {
            if (update == null) {
                infoTextView.setText("Error occurred, check logs.");
            } else {
                infoTextView.setText(update);
            }
        }
    }
}

