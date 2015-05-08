package redstonelamp.clock;

import java.util.ArrayList;

/**
 * Ticker class. Acts as a clock for the server.
 */
public class RedstoneTicker extends Thread{
    private long currentTick = 0;
    private ArrayList<Task> tasks = new ArrayList<>();
    private int tps;
    private int nextTaskID = 0;
    private boolean running;

    public RedstoneTicker(int tps){
        this.tps = tps;
    }

    public void start(){
        running = true;
        super.start();
    }

    public void Stop() throws InterruptedException {
        running = false;
        join();
    }

    public void run(){
        while(running){
            currentTick++;
            for(Task task : tasks){
                if(task.getLastTickRan() - currentTick >= task.getDelay()){
                    task.run();
                    task.setLastTickRan(currentTick);
                }
            }

            try {
                sleep(1000 / tps);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int registerTask(Task task){
        task.setTaskID(nextTaskID++);
        synchronized (tasks){
            tasks.add(task);
        }
        return task.getTaskID();
    }

    public boolean cancelTask(Task task){
        synchronized (tasks){
            return tasks.remove(task);
        }
    }

    public boolean cancelTask(int taskId){
        synchronized (tasks){
            for(Task task : tasks){
                if(task.getTaskID() == taskId){
                    return tasks.remove(task);
                }
            }
        }
        return false;
    }
}
