/*

    ApkPopper "Simple App that lets user to get info about installed app and extract them."
    Copyright (C) 2019  Sujan Thapa

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */

package com.ztcartxe.reppopkpa.apkpopper.engine;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

public class PopManager{

    private static final String TAG = "PopManager";

    private File fileSource;
    private Context context;
    private String path, fileName;
    private ProgressCallBack progressCallBack;
    FileInputStream fileInputStream;
    FileOutputStream fileOutputStream;

    public interface ProgressCallBack{
        void onStart();
        void onProgress(CallbackByteChannel cbc, double progress);
        void onStop();
    }

    public PopManager(Context context, File fileSource, String path, String fileName, ProgressCallBack progressCallBack) {
        this.context = context;
        this.fileSource = fileSource;
        this.path = path;
        this.fileName = fileName;
        this.progressCallBack = progressCallBack;
    }

    public void ExtractApk(){
        try{
            File fileDestination;
            File dir = new File(this.path);
            if(dir.exists() && dir.isDirectory()){
                fileDestination = new File(path  + fileName);
                Log.e(TAG, "exists");
            }
            else{
                //Create Directory
                boolean data= dir.mkdir();
                fileDestination = new File(path  + fileName);
                Log.e(TAG, "mkdir()-->" + data);
            }
            pop(fileDestination);
        }
        catch (Exception ex){
            Log.e(TAG, ex.toString());
            ex.printStackTrace();
        }
    }

    private void pop(File fileDestination){
        try {
            progressCallBack.onStart();
            fileInputStream = new FileInputStream(fileSource);
            fileOutputStream = new FileOutputStream(fileDestination);

            FileChannel fileChannel_IN = fileInputStream.getChannel();
            FileChannel fileChannel_OUT = fileOutputStream.getChannel();

            WritableByteChannel writableByteChannel = new CallbackByteChannel(fileChannel_OUT, fileChannel_IN.size(), progressCallBack);

            fileChannel_IN.transferTo(0, fileChannel_IN.size(), writableByteChannel);

            fileChannel_IN.close();
            fileChannel_OUT.close();

            Toast.makeText(context, "APK Extracted : " + fileDestination.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        }
        catch (Exception ex){
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            progressCallBack.onStop();
            ex.printStackTrace();
        }
        finally {
            try {if (fileInputStream != null) {fileInputStream.close();}if (fileOutputStream != null) {fileOutputStream.close();}} catch (Exception ex){ex.printStackTrace();}
            progressCallBack.onStop();
        }
    }

    public class CallbackByteChannel implements WritableByteChannel{
        ProgressCallBack delegate;
        long size;
        WritableByteChannel wbc;
        long sizeWrite;

        CallbackByteChannel(WritableByteChannel wbc, long size, ProgressCallBack delegate) {
            this.wbc = wbc;
            this.size = size;
            this.delegate = delegate;
        }

        @Override
        public boolean isOpen() {
            return wbc.isOpen();
        }

        @Override
        public void close() throws IOException {
            wbc.close();
        }

        @Override
        public int write(ByteBuffer byteBuffer) throws IOException {
            int n;
            double progress;
            if((n = wbc.write(byteBuffer)) > 0){
                sizeWrite += n;
                progress = size > 0 ? (double) sizeWrite / (double) size * 100.0 : -1.0;
                delegate.onProgress(this, progress);
            }
            return n;
        }
    }
}