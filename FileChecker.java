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

                /*
                if(i > DuplicateFinder.progressBar.getValue()){
                    synchronized (this) {
                        DuplicateFinder.progressBar.setValue(i);
                    }
                }
                */
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
