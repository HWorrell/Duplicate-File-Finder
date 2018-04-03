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
