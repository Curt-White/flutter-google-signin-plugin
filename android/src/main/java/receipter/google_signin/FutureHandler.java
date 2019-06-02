package receipter.google_signin;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import android.os.Handler;
import android.os.Looper;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import io.flutter.plugin.common.MethodChannel.Result;


public class FutureHandler {
    private final ThreadPoolExecutor exec;
    private Operation currentOp;

    // Represents an opereration
    public class Operation {
        Result result;
        String opName;
        Object data;

        public Operation(Result result, String opName, Object data) {
            this.result = result;
            this.opName = opName;
            this.data = data;
        }
    }

    /**
     * Set the current operation and throw an exception if there is already an op set
     * @param result - A Flutter result object
     * @param opName - The name of the method currently running
     * @param data - The data associated with the current operation
     */
    public void setOperation(Result result, String opName, Object data)  {
        if (this.currentOp != null) {
            throw new IllegalStateException("Multiple concurrent operations detected");
        }

        this.currentOp = new Operation(result, opName, data);
    }

    /**
     * Get the current running operation
     * @return an Operation object returning the current operation
     */
    public Operation getOperation()  {
        return this.currentOp;
    }

    /**
     * Clear the current operation
     */
    public void clearOperation()  {
        this.currentOp = null;
    }

    /**
     * Finish the current operation with success and clear operation
     * @param data - The data to be returned to flutter on completion of operation
     */
    public void finishOperationWithSuccess(Object data) {
        this.currentOp.result.success(data);
        this.currentOp = null;
    }

    /**
     * Finish the current operation with an error
     * @param code - The error code to be returned
     * @param message - The error message to return
     */
    public void finishOperationWithError(String code, String message) {
        this.currentOp.result.error(code, message, null);
        this.currentOp = null;
    }

    /**
     * Represents a callback funtion for an async operation
     * @param <T> - The type of the future to be handled in the callbacks run function
     */
    public interface Callback<T> {
        void run(Future<T> future);
    }

    /**
     * Constructor for the future handler
     * @param threads - number of threads to be used in the async handler
     */
    public FutureHandler(int threads) {
        exec = new ThreadPoolExecutor(threads, threads, 1500, TimeUnit.MILLISECONDS, 
                    new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Handler the async funcntions and callback for that function
     * @param <T> - Type of the value to be handled in func and callback
     * @param func - The function being called asynchronously
     * @param callback - The callback funtion called upon completion of func
     */
    public <T> void handleAsync(final Callable<T> func, final Callback<T> callback) {
        final ListenableFuture<T> future = runAsync(func);

        future.addListener(
            new Runnable(){
                @Override
                public void run() {
                    callback.run(future);
                }
            },
            new Executor(){
                @Override
                public void execute(Runnable command) {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(command);
                }
            }
        );
    }

    /**
     * Run a function asynchronously and return a listenable future
     * @param <T> - the type of the listendable future being returned
     * @param func - the function to be called asynchronously
     * @return a listenable future
     */
    public <T> ListenableFuture<T> runAsync(final Callable<T> func) {
        final SettableFuture<T> future = SettableFuture.create();

        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!future.isCancelled()) {
                    try {
                        future.set(func.call());
                    } catch (Throwable t) {
                        future.setException(t);
                    }
                }
            }
        });

        return future;
    }
}