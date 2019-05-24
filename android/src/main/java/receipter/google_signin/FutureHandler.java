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

    public void setOperation(Result result, String opName, Object data)  {
        if (this.currentOp != null) {
            throw new IllegalStateException("Multiple concurrent operations detected");
        }

        this.currentOp = new Operation(result, opName, data);
    }

    public Operation getOperation()  {
        return this.currentOp;
    }

    public finishOperationWithSuccess(Object data) {
        this.currentOp.result.success(data);
        this.currentOp = null;
    }

    public finishOperationWithError(String code, String message) {
        this.currentOp.result.error(code, message, null);
        this.currentOp = null;
    }

    public interface Callback<T> {
        void task(Future<T> future);
    }

    public FutureHandler(int threads) {
        exec = new ThreadPoolExecutor(threads, threads, 1500, TimeUnit.MILLISECONDS, 
                    new LinkedBlockingQueue<Runnable>());
    }

    public <T> void HandleAsync(final Callable<T> func, final Callback<T> callback) {
        final ListenableFuture<T> future = RunAsync(func);

        future.addListener(
            new Runnable(){
                @Override
                public void run() {
                    callback.task(future);
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

    public <T> ListenableFuture<T> RunAsync(final Callable<T> func) {
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