import com.sun.org.apache.bcel.internal.generic.DUP;
import org.apache.commons.io.IOUtils;
import sun.nio.ch.IOUtil;

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
        String drivesToCheck = JOptionPane.showInputDialog("Enter the drive letters to be checked, separated by a space.\r\n" +
                "Ex: C D E");
        String maxSize = JOptionPane.showInputDialog("Enter the largest file size in Megabytes that will be checked.\r\n" +
                "(If this size is large, the running time will be very large also.");
        String colonslash = ":\\";
        long start, stop, elapsed;
        start = System.currentTimeMillis();
        Vector<String> pathToFile = new Vector<>();
        Vector<String> fileName = new Vector<>();

        Vector<String> hashes = new Vector<>();
        String[] drives = drivesToCheck.split(" ");
        File[] startFile = new File[drives.length];

        for(int i = 0; i < drives.length; i++){
            startFile[i] = new File(drives[i] + colonslash);
        }
        /*

        Recursively find all the files under the path(s) given

         */
        for (int i = 0; i < startFile.length; i++) {
            fileExplorer(startFile[i], pathToFile, fileName, Long.parseLong(maxSize), oversizedPathToFile, oversizedFileName);
            System.out.println("Drive" + (i + 1) + " checked.");
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
            threads[i] = new FileChecker(i * (fileName.size() / numThreads), (i + 1) * (fileName.size() / numThreads), fileName, pathToFile, i, hashes, numThreads, sizeLimit, oversizedPathToFile, oversizedFileName);
        }

        final JFrame frame = new JFrame("Duplicate Checker Progress");

        progressBar = new JProgressBar();

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

        //ensure all threads have completed before moving on
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //sort vector of hashes

        Vector<String> sortedHashes = (Vector<String>) hashes.clone();

        Collections.sort(sortedHashes);

        //create hashmap
        TreeSet<String> duplicateHashes = new TreeSet<>();

        //iteratively move through hash vector.  If item and item + 1 have same hash, push to hashmap

        for(int i = 0; i < hashes.size() - 1; i++){
            if(hashes.elementAt(i).equals(hashes.elementAt(i + 1))){
                duplicateHashes.add(hashes.elementAt(i));
            }
        }

        for (String s: duplicateHashes
                ) {
            if (!s.equals("")) {
                String temp = "Duplicates located at: ";
                for (int i = 0; i < hashes.size(); i++) {
                    if (hashes.elementAt(i).equals(s)) {
                        temp += pathToFile.elementAt(i) + ", ";
                    }

                }
                duplicates.add(temp);
            }
        }

        writeResults();

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
                    fileExplorer(f, pathToFile, fileName, fileSize, overPathToFile, overFileName);
                }
            }
        }
    }
    static void writeResults(){

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
            out.append("The following files are too large to be checked.\r\nA manual inspection is required:\r\n");
            for (int i = 0; i < oversizedPathToFile.size(); i++) {
                out.append(oversizedPathToFile.elementAt(i));
                out.append("\r\n\r\n");
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

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

    FileChecker(int s, int st, Vector name, Vector path, int myID, Vector hash, int numT, long sizeLimit, Vector oPath, Vector oName) {
        this.start = myID;
        this.stop = st;
        this.fNames = name;
        this.paths = path;
        this.ID = myID;
        this.hashVector = hash;
        this.totalThreads = numT;
        this.fileSizeLimit = sizeLimit;
        this.oPaths = oPath;
        this.oFNames = oName;
    }

    @Override
    public void run() {
        //for each file in scope, determine the hash of the file contents

        String md5 = "";
        for (int i = start; i < paths.size(); i += this.totalThreads) {
            if(new File(paths.elementAt(i)).length() < 1000000 * fileSizeLimit) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(new File(paths.elementAt(i)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    md5 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(fis);
                } catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //store hash in vector provided

                hashVector.set(i, md5);

                //System.out.println

                //System.out.println(i + ": " + md5 + " : " + paths.elementAt(i));
                if(i % 100 == 0){
                    DuplicateFinder.progressBar.setValue(i);
                }

            }
            /*
            else{
                DuplicateFinder.oversized.add(paths.elementAt(i));
            }
            */
        }
    }
}

