package com.remoteyourcam.usb.util;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class Upload extends AsyncTask<Bitmap, String, String> {

    public AsyncResponse delegate = null;

    public void setDelegate(AsyncResponse delegate) {
        this.delegate = delegate;
    }

    private static List<File> uploadQueue;

    public static List<File> getUploadQueue() {
        return uploadQueue;
    }

    public static void addFileToUploadQUeue(File file) {
        uploadQueue.add(file);
    }

    public String doInBackground(Bitmap... images) {
        Log.i("UPLOAD", "Starting....");
        Bitmap image = images[0];
        try {
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            //todo change URL as per client ( MOST IMPORTANT )
            URL url = new URL("https://weddingapi.never-al.one/slideshow/uploadImage");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs &amp; Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            FileInputStream fileInputStream;
            DataOutputStream outputStream;
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);

            outputStream.writeBytes("Content-Disposition: form-data; name=\"device_id\""+ lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes("Photoboothie");
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);

            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + "File.jpg" +"\"" + lineEnd);
            outputStream.writeBytes(lineEnd);

            image.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String result = null;
            if (serverResponseCode == 200) {
                StringBuilder s_buffer = new StringBuilder();
                InputStream is = new BufferedInputStream(connection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    s_buffer.append(inputLine);
                }
                result = s_buffer.toString();
            }
            outputStream.flush();
            outputStream.close();
            if (result != null) {
                Log.d("result_for upload", result);
                if (delegate != null) {
                    delegate.processFinish(result);
                }
                return result;
                //file_name = getDataFromInputStream(result, "file_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Blub";
        //return file_name;
    }
}
