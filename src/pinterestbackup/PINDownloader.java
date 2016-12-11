/*
 * The MIT License
 *
 * Copyright 2016 Stefano Vannucci.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package pinterestbackup;

import pinterestbackup.exceptions.PinCopyException;
import com.pinterestweblibrary.PinterestBoard;
import com.pinterestweblibrary.PinterestPin;
import com.pinterestweblibrary.http.PinterestHTTPConnector;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Task to download pins from a given board
 * 
 * @author Stefano
 */
public class PINDownloader implements Runnable {
    
    private final PinterestBoard board;
    private BackupConfiguration config = null;
    private HashMap <String,Boolean> localFiles;
    private final Logger logPIN = Logger.getLogger(PINDownloader.class.getName());
    
    public PINDownloader (PinterestBoard pinB, BackupConfiguration configuration){
        board = pinB;
        config = configuration;
        localFiles = new HashMap<>();
        if (config.isVerbose()){
            logPIN.setFilter(new BackupLoggerFilter(config.isVerbose()));
        }
    }
    
    @Override
    public void run() {
        
        try {
            Path boardPath = null;
            Path pinPath = null;
            String [] urlPath = null;
            String pinName = "";
            PinterestHTTPConnector pinterestResource = new PinterestHTTPConnector();
            boolean foundOriginal = false;
            
            //Check directory exists
            boardPath = Paths.get(config.getPathSave().toString(), board.getSlug());
            if (!Files.exists(boardPath)){
                try {
                    logPIN.log(Level.INFO, "Create new folder in {0}", boardPath.toString());
                    Files.createDirectory(boardPath);
                } catch (IOException ex) {
                    Logger.getLogger(PinterestBackup.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            //Populate directory files hashmap
            logPIN.log(Level.INFO, "Path files analysis.");
            this.populateFileMap(boardPath);
            
            logPIN.log(Level.INFO, "Retrieving pins for board {0}.", board.getName());
            pinterestResource.getBoardPins(board);
            
            Iterator<PinterestPin> iterPins = board.getPins().iterator();
            while (iterPins.hasNext()){
                PinterestPin currPin = iterPins.next();
                pinPath = Paths.get(boardPath.toString());
                urlPath = currPin.getUrl().split("/");
                foundOriginal = false;
                pinName = "";
                for (String urlPath1 : urlPath) {
                    if (foundOriginal) {
                        pinName += urlPath1;
                    }
                    if (urlPath1.equals("originals")) {
                        foundOriginal = true;
                    }
                }
                
                if (foundOriginal){
                    pinPath = Paths.get(pinPath.toString(),pinName);
                    savePinToFile(currPin,pinPath);
                    this.setFileMapFound(pinPath);
                }
            }
            
            this.removeDeletedPins();
        } catch (PinCopyException |URISyntaxException | IOException ex) {
            Logger.getLogger(PINDownloader.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    private void populateFileMap(Path pathDir) {       
        //Read all file in pathDir and store in the local hashmap
        this.localFiles.clear();
        
        if (pathDir != null){
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream( pathDir );
                for (Path path : stream) {
                    this.localFiles.put(path.toString(), Boolean.FALSE);
                }
            } catch (IOException ex) {
                Logger.getLogger(PinterestBackup.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void setFileMapFound(Path pinPath) {
        if (!config.isSynchronized()){
            return;
        }
        this.localFiles.put(pinPath.toString(), Boolean.TRUE);
    }
    
    private void removeDeletedPins() {
        
        if (!config.isSynchronized()){
            return;
        }

        Path deleteFile = null;
        if (this.localFiles != null && this.localFiles.size() > 0){
            for (Map.Entry<String,Boolean> entry : this.localFiles.entrySet()){
                if (!entry.getValue()){
                    deleteFile = Paths.get(entry.getKey());
                    try {
                        Files.delete(deleteFile);
                    } catch (IOException ex) {
                        Logger.getLogger(PinterestBackup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * This method retrieve a pin connecting via HTTP to URLSource and copy the content
     * to the specified pathSave.
     * 
     * @param pin
     * @param pinPath: Path to save
     * @throws pinterestbackup.PinCopyException
     */
    public void savePinToFile(PinterestPin pin, Path pinPath) throws PinCopyException{
        HttpGet httpgetImg = new HttpGet();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        
        if (pinPath == null)
            return;
        
        //Check if file exists
        if (this.localFiles.containsKey(pinPath.toString())){
        //if (Files.exists(pinPath)){
            logPIN.log(Level.INFO, "Board {0} PIN {1} already exists.", new Object[]{board.getName(), pin.getUrl()});
            return;
        }
        
        logPIN.log(Level.INFO, "Save Board {0} PIN {1} to path.", new Object[]{board.getName(), pin.getUrl()});
        
        try {
            URI uri = new URI(pin.getUrl());
            httpgetImg.setURI(uri);
            CloseableHttpResponse response = httpclient.execute(httpgetImg);
            try (InputStream instream = response.getEntity().getContent()) {
                Files.copy(instream, pinPath);
            }
        } catch (IOException | URISyntaxException ex) {
            throw new PinCopyException(ex.getMessage());
        }
    }
}
