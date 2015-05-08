package redstonelamp.clock;


/**
 * Created by jython234 on 5/7/2015.
 */
public abstract class Task implements Runnable{
    private int taskID;
    private int delay;
    private long lastTickRan;
    private final RedstoneTicker ticker;

    public Task(RedstoneTicker ticker, int delay){
        this.ticker = ticker;
        this.delay = delay;
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public long getLastTickRan() {
        return lastTickRan;
    }

    protected void setLastTickRan(long lastTickRan) {
        this.lastTickRan = lastTickRan;
    }

    public RedstoneTicker getTicker() {
        return ticker;
    }
}
