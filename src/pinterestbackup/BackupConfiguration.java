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

import java.nio.file.Path;

/**
 * Singleton configuration
 * 
 * @author Stefano Vannucci
 */
public class BackupConfiguration {
    
    public final static int DEFAULT_READ_RETRY_ERROR = 3;
    
    private static BackupConfiguration instance = null;
    private final String userName;
    private final Path pathSave;
    private final boolean verbose;
    private final boolean synch;
    private final int readRetry;
    
    private BackupConfiguration(String userName, Path pathSave, boolean isVerbose, boolean isSynchronized, int readRetry){
        this.userName = userName;
        this.pathSave = pathSave;
        this.verbose = isVerbose;
        this.synch = isSynchronized;
        this.readRetry = readRetry;
    }
        
    public static BackupConfiguration getInstance(String userName, Path pathSave, boolean isVerbose, boolean isSynchronized, int readRetry){
        if (instance == null)
            instance = new BackupConfiguration(userName, pathSave,isVerbose, isSynchronized, readRetry);
        return instance;
    }
    
    public static BackupConfiguration getInstance(String userName, Path pathSave, boolean isVerbose, boolean isSynchronized){
        if (instance == null)
            instance = new BackupConfiguration(userName, pathSave,isVerbose, isSynchronized, DEFAULT_READ_RETRY_ERROR);
        return instance;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isSynchronized() {
        return synch;
    }

    public String getUserName() {
        return userName;
    }

    public Path getPathSave() {
        return pathSave;
    }
    
    public int getReadRetry(){
        return readRetry;
    }
    
}
