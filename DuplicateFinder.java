import javax.swing.*;
import java.awt.*;
import java.io.*;

import java.util.Collections;
import java.util.TreeSet;
import java.util.Vector;

public class DuplicateFinder {
    static Vector<String> duplicates = new Vector<>();
    static JProgressBar progressBar;
    static Vector<String> oversized = new Vector<>();
    static Vector<String> oversizedPathToFile = new Vector<>();
    static Vector<String> oversizedFileName = new Vector<>();
    static Vector<String> hashLogTesting = new Vector<>();
    public static void main(String[] args) throws IOException {
        String input = JOptionPane.showInputDialog("Enter 1 to use minimal processing power, 2 to use half.\r\n" +
                "Entering any other value will set the program to maximum value\r\nThis application is not especially CPU intensive, but using more may hinder resource-intensive applications.");
        Vector<String> drivesToCheck = new Vector<>();
        String dr = "0";
        while(!dr.equals("")){
            dr = JOptionPane.showInputDialog("Enter the drive letter to be checked, one at a time.\r\n" +
                    "Ex: C\r\nIf you want to check folders, instead enter the complete path to the folder." +
                    "\r\nEx: C:\\Users\\<username>\\Documents\r\n" +
                    "When you have entered all of your selections, press enter without entering anything.");
            if(dr.equals("")){
                break;
            }
            drivesToCheck.add(dr);
        }

        String maxSize = JOptionPane.showInputDialog("Enter the largest file size in Megabytes that will be checked.\r\n" +
                "(If this size is large, the running time will be very large also.");
        String colonslash = ":\\";
        long start, stop, elapsed;
        start = System.currentTimeMillis();
        Vector<String> pathToFile = new Vector<>();
        Vector<String> fileName = new Vector<>();

        Vector<String> hashes = new Vector<>();
        File[] startFile = new File[drivesToCheck.size()];

        for(int i = 0; i < drivesToCheck.size(); i++){
            if(drivesToCheck.elementAt(i).length() == 1) {
                startFile[i] = new File(drivesToCheck.elementAt(i) + colonslash);
            }
            else{
                if(drivesToCheck.elementAt(i).contains(":\\")){
                    startFile[i] = new File(drivesToCheck.elementAt(i));
                }
            }
        }
        /*

        Recursively find all the files under the path(s) given

         */
        for (int i = 0; i < startFile.length; i++) {
            fileExplorer(startFile[i], pathToFile, fileName, Long.parseLong(maxSize), oversizedPathToFile, oversizedFileName);
            System.out.println("Drive " + (i + 1) + " checked.");
        }
        System.out.println(pathToFile.size());

        //System.out.println(fileName.size());

        //numThreads

        int numThreads;

        if(input.equals("1")){
            numThreads = 1;
        }
        else if(input.equals("2")){
            numThreads = Runtime.getRuntime().availableProcessors()/2;
        }
        else if(input.equals("10")){
            numThreads = Runtime.getRuntime().availableProcessors() * 10;
        }
        else{
            numThreads = Runtime.getRuntime().availableProcessors();
        }
        hashes.ensureCapacity(pathToFile.size());

        for(int i = 0; i < pathToFile.size(); i++){
            hashes.add("");
        }

        //create thread array
        FileChecker[] threads = new FileChecker[numThreads];
        //create threads
        long sizeLimit = Long.parseLong(maxSize);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new FileChecker(i * (fileName.size() / numThreads), (i + 1) * (fileName.size() / numThreads), fileName, pathToFile, i, hashes, numThreads, sizeLimit);
        }

        WatcherThread watcher = new WatcherThread(threads);

        final JFrame frame = new JFrame("Duplicate Checker Progress");

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        frame.setLayout(new FlowLayout());
        frame.getContentPane().add(progressBar);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setVisible(true);

        progressBar.setMinimum(0);
        progressBar.setMaximum(pathToFile.size());

        //start threads
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        watcher.start();

        //ensure all threads have completed before moving on
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            watcher.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //sort vector of hashes

        Vector<String> sortedHashes = (Vector<String>) hashes.clone();

        System.out.println("Hashes Duplicated");

        Collections.sort(sortedHashes);

        System.out.println("Hashes Sorted");
        //create hashmap
        TreeSet<String> duplicateHashes = new TreeSet<>();

        //iteratively move through hash vector.  If item and item + 1 have same hash, push to hashmap

        for(int i = 0; i < hashes.size() - 1; i++){
            if(sortedHashes.elementAt(i).equals(sortedHashes.elementAt(i + 1))){
                duplicateHashes.add(sortedHashes.elementAt(i));
                System.out.println("Duplicate found at " + i);
            }
        }

        frame.remove(progressBar);

        progressBar = new JProgressBar();

        frame.add(progressBar);

        progressBar.setMinimum(0);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setMaximum(duplicateHashes.size());

        int count = 0;

        for (String s: duplicateHashes
                ) {
            System.out.println("Checking duplicate " + s);
            if (!s.equals("")) {
                String temp = "Duplicates located at: \r\n";
                for (int i = 0; i < hashes.size(); i++) {
                    if (hashes.elementAt(i).equals(s)) {
                        temp += pathToFile.elementAt(i) + ",\r\n";
                    }

                }
                System.out.println("Checked " + count + " of " + duplicateHashes.size());
                ++count;
                progressBar.setValue(count);
                duplicates.add(temp);
            }
        }

        writeResults(pathToFile, fileName);

        stop = System.currentTimeMillis();

        elapsed = stop - start;

        System.out.println("Time elapsed: " + elapsed / 1000);
    }
    static void fileExplorer(File head, Vector<String> pathToFile, Vector<String> fileName, long fileSize, Vector<String> overPathToFile, Vector<String> overFileName) {
        File[] contents = head.listFiles();

        if (contents != null) {
            for (File f : contents
                    ) {
                if (!f.isDirectory() && f.length() < fileSize * 1000000) {
                    pathToFile.add(f.getPath());
                    fileName.add(f.getName());
                }
                else if(!f.isDirectory()){
                    overPathToFile.add(f.getPath());
                    overFileName.add(f.getName());
                }
                else {
                    if(!f.getPath().contains("\\Windows\\")) {
                        fileExplorer(f, pathToFile, fileName, fileSize, overPathToFile, overFileName);
                    }
                }
            }
        }
    }
    static void writeResults(Vector pathsIn, Vector fileNames){

        File output = new File("Duplicate Report.txt");
        PrintWriter out = null;
        try {
            out = new PrintWriter(output);
            for (int i = 0; i < duplicates.size(); i++) {
                out.append(duplicates.elementAt(i));
                out.append("\r\n\r\n");
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File oversizedOutput = new File("Oversized Files.txt");
        PrintWriter outOver = null;
        try {
            out = new PrintWriter(oversizedOutput);
            out.append("The following files are too large to be checked by the settings use in this run.\r\nA manual inspection is required:\r\n");
            for (int i = 0; i < oversizedPathToFile.size(); i++) {
                out.append(oversizedPathToFile.elementAt(i));
                out.append("\r\n\r\n");
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File allFilesChecked = new File("FileList.txt");
        PrintWriter allFiles = null;
        try{
            allFiles = new PrintWriter(allFilesChecked);
            allFiles.append("The following is a list of all files checked in this run:\r\n");
            for(int i = 0; i < pathsIn.size(); i++){
                allFiles.append("File name: " + fileNames.elementAt(i) + "\r\n");
                allFiles.append("Path to file: " + pathsIn.elementAt(i) + "\r\n\r\n");
            }
            allFiles.close();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }

        System.exit(0);
    }
}
class FileChecker extends Thread {
    int start;
    int stop;
    int ID;
    int totalThreads;
    long fileSizeLimit;
    Vector<String> fNames;
    Vector<String> paths;
    Vector<String> hashVector;
    Vector<String> oPaths;
    Vector<String> oFNames;
    int i;

    FileChecker(int s, int st, Vector name, Vector path, int myID, Vector hash, int numT, long sizeLimit) {
        this.start = myID;
        this.stop = st;
        this.fNames = name;
        this.paths = path;
        this.ID = myID;
        this.hashVector = hash;
        this.totalThreads = numT;
        this.fileSizeLimit = sizeLimit;
    }

    public int getI(){
        return this.i;
    }

    @Override
    public void run() {
        //for each file in scope, determine the hash of the file contents

        String md5 = "";
        for (i = start; i < paths.size(); i += this.totalThreads) {
            md5 = "";
            if(new File(paths.elementAt(i)).length() < 1000000 * fileSizeLimit) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(new File(paths.elementAt(i)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    md5 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(fis);
                    fis.close();
                } catch (Exception e){
                    e.printStackTrace();
                }


                //store hash in vector provided

                hashVector.set(i, md5);

                //System.out.println

                //System.out.println(i + ": " + md5 + " : " + paths.elementAt(i));
                if(i > DuplicateFinder.progressBar.getValue()){
                    synchronized (this) {
                        DuplicateFinder.progressBar.setValue(i);
                    }
                }

                /*testing*/
                //if(i % 100 == 0 || i % 101 == 0 || i % 102 == 0 || i % 103 == 0) {
                    System.out.println(ID + " " + i + " " + paths.elementAt(i));
                //}
                /**/
            }
            /*
            else{
                DuplicateFinder.oversized.add(paths.elementAt(i));
            }
            */
        }

    }
}
class WatcherThread extends Thread{

    FileChecker[] arr;
    WatcherThread(FileChecker[] arrIn){
        this.arr = arrIn;
    }

    boolean isLive(){
        boolean isLive = false;

        for(int i = 0; i < arr.length; i++){
            if(arr[i].isAlive()){
                isLive = true;
            }
        }

        return isLive;

    }

    @Override
    public void run() {

        while (isLive()) {
            int average = 0;
            for (int i = 0; i < arr.length; i++) {
                average += arr[i].getI();
            }
            average /= arr.length;


            DuplicateFinder.progressBar.setValue(average);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
