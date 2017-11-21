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

import pinterestbackup.exceptions.BoardRetrieveException;
import com.pinterestweblibrary.PinterestBoard;
import com.pinterestweblibrary.http.PinterestHTTPConnector;
import com.pinterestweblibrary.http.UserNameNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stefano
 */
public class PinterestBackup {

    private static void PrintHelp(){
        System.out.println("PinterestBackup Username DestinationPath [-s] [-v] [-r].");
        System.out.println("");
        System.out.println("Username:          Pinterest user to backup");
        System.out.println("DestinationPath:   Path to store backup images");
        System.out.println("-s:                Syncronize local images with account. WARNING this option delete local files if the images are not pinned anymore.");
        System.out.println("-v:                Verbose mode off.");
        System.out.println("-r:                Number of attempts in case of PIN read error. Optional default " + BackupConfiguration.DEFAULT_READ_RETRY_ERROR + ".");
    }
    
    private static boolean checkArgs(String[] args) {
        
        boolean isVerbose = true;
        boolean isSynchronized = false;
        int iAttempts = BackupConfiguration.DEFAULT_READ_RETRY_ERROR;
        Path destPath = null;
        
        if (args.length < 2){
            System.err.println("Missing arguments.\n");
            PrintHelp();
            return false;
        }
        
        try{
            destPath = Paths.get(args[1]);
        }
        catch(Exception e){
            System.err.println("Unrecognized path save. " + e.getMessage());
            PrintHelp();
            return false;
        }
        
        //Check destination path
        if (!Files.exists(destPath)){
            System.err.println("Destination path doesn't exists.\n");
            PrintHelp();
            return false;
        }
        
        for (int i=2; i<args.length; i++){
            if (args[i].equals("-s"))
                isSynchronized = true;
            if (args[i].equals("-v"))
                isVerbose = false;
            if (args[i].startsWith("-r")){
                String arg = args[i].replace("-r", "");
                try{
                    iAttempts = Integer.parseInt(args[i].replace("-r", ""));
                }
                catch(NumberFormatException ex){
                    System.err.println("Invalid number of attempts supplied.\n");
                    PrintHelp();
                    return false;
                }
            }
        }
        
        config = BackupConfiguration.getInstance(args[0], destPath, isVerbose, isSynchronized, iAttempts);
        logBackup.setFilter(new BackupLoggerFilter(isVerbose));

        return true;
    }
    
    private static final Logger logBackup = Logger.getLogger(PinterestBackup.class.getName());
    private static BackupConfiguration config = null;
    
    public PinterestBackup(){
        
    }
    
    public PinterestBackup(BackupConfiguration configuration){
        config = configuration;
    }
    
    public static void main(String[] args){
        
        if (!checkArgs(args))
            return;
                
        PinterestBackup backup = new PinterestBackup();
        try {
            backup.doBackup();
        } catch (BoardRetrieveException ex) {
            System.err.println("Errore during backup: " + ex.getMessage());
        }
    }
    
    public void doBackup() throws BoardRetrieveException {        
        ArrayList<PinterestBoard> userBoards = null;
        PinterestHTTPConnector pinterestResource = new PinterestHTTPConnector();
        
        try {
            //Get user boards
            logBackup.log(Level.INFO, "Start retrieving boards for user {0}", config.getUserName());
            userBoards = pinterestResource.retrieveBoards(config.getUserName());
        } catch (IOException | URISyntaxException | UserNameNotFoundException ex) {
            throw new BoardRetrieveException("Error in retrieving boards: " + ex.getMessage());
        }
        
        logBackup.log(Level.INFO, "Found {0} boards for user {1}", new Object[]{userBoards.size(), config.getUserName()});
        
        int CPUCore = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(CPUCore * 2);
        
        logBackup.log(Level.INFO, "Start Pin backup");
        
        Iterator<PinterestBoard> iter = userBoards.iterator();
        while (iter.hasNext()){
            PinterestBoard board = iter.next();
            
            Runnable worker = new PINDownloader(board,config);
            executor.execute(worker);
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            logBackup.log(Level.SEVERE, null, ex);
        }
        finally{
            logBackup.log(Level.INFO, "Backup complete");
        }
    }
}
