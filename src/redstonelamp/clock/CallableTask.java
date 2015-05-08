package redstonelamp.clock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents a callable task that can be called.
 */
public class CallableTask extends Task{
    private Object instance;
    private Method method;

    public CallableTask(RedstoneTicker ticker, int delay, String methodName, Object instance) throws NoSuchMethodException {
        super(ticker, delay);
        method = instance.getClass().getMethod(methodName);
        instance = instance;
    }

    @Override
    public void run() {
        try {
            method.invoke(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Error while running CallableTask (InvocationTargetException).");
            e.printStackTrace();
        }
    }
}
