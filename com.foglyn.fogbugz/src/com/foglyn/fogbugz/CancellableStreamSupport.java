package com.foglyn.fogbugz;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

class CancellableStreamSupport {
    private final IProgressMonitor monitor;
    private final ExecutorService service;

    CancellableStreamSupport(IProgressMonitor monitor, ThreadFactory factory) {
        Utils.assertNotNullArg(monitor, "monitor");
        Utils.assertNotNullArg(factory, "factory");
        
        this.monitor = monitor;
        this.service = Executors.newSingleThreadExecutor(factory);
    }

    <T> T submit(Callable<T> task) throws IOException {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
            
        Future<T> future = service.submit(task);

        T data = null;
        boolean canceled = false;
        while (true) {
            if (monitor.isCanceled()) {
                canceled = true;
                future.cancel(true);
            }
            
            try {
                data = future.get(250, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException e) {
                throw new InterruptedIOException("IO interrupted");
            } catch (ExecutionException e) {
                throw new CancellableIOException("Exception returned from other thread", e.getCause());
            } catch (CancellationException e) {
                throw new OperationCanceledException();
            } catch (TimeoutException e) {
                if (canceled) {
                    throw new OperationCanceledException();
                }
                
                // try again
            }
        }
        
        return data;
    }

    void shutdown() {
        service.shutdown();
    }
}
