/*
 * Copyright 2005-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.java2d.opengl;

import sun.java2d.pipe.RenderBuffer;
import sun.java2d.pipe.RenderQueue;
import static sun.java2d.pipe.BufferedOpCodes.*;

/**
 * OGL-specific implementation of RenderQueue.  This class provides a
 * single (daemon) thread that is responsible for periodically flushing
 * the queue, thus ensuring that only one thread communicates with the native
 * OpenGL libraries for the entire process.
 */
public class OGLRenderQueue extends RenderQueue {

    private static OGLRenderQueue theInstance;
    private final QueueFlusher flusher;

    private OGLRenderQueue() {
        flusher = new QueueFlusher();
    }

    /**
     * Returns the single OGLRenderQueue instance.  If it has not yet been
     * initialized, this method will first construct the single instance
     * before returning it.
     */
    public static synchronized OGLRenderQueue getInstance() {
        if (theInstance == null) {
            theInstance = new OGLRenderQueue();
        }
        return theInstance;
    }

    /**
     * Flushes the single OGLRenderQueue instance synchronously.  If an
     * OGLRenderQueue has not yet been instantiated, this method is a no-op.
     * This method is useful in the case of Toolkit.sync(), in which we want
     * to flush the OGL pipeline, but only if the OGL pipeline is currently
     * enabled.  Since this class has few external dependencies, callers need
     * not be concerned that calling this method will trigger initialization
     * of the OGL pipeline and related classes.
     */
    public static void sync() {
        if (theInstance != null) {
            theInstance.lock();
            try {
                theInstance.ensureCapacity(4);
                theInstance.getBuffer().putInt(SYNC);
                theInstance.flushNow();
            } finally {
                theInstance.unlock();
            }
        }
    }

    /**
     * Disposes the native memory associated with the given native
     * graphics config info pointer on the single queue flushing thread.
     */
    public static void disposeGraphicsConfig(long pConfigInfo) {
        OGLRenderQueue rq = getInstance();
        rq.lock();
        try {
            // make sure we make the context associated with the given
            // GraphicsConfig current before disposing the native resources
            OGLContext.setScratchSurface(pConfigInfo);

            RenderBuffer buf = rq.getBuffer();
            rq.ensureCapacityAndAlignment(12, 4);
            buf.putInt(DISPOSE_CONFIG);
            buf.putLong(pConfigInfo);

            // this call is expected to complete synchronously, so flush now
            rq.flushNow();
        } finally {
            rq.unlock();
        }
    }

    /**
     * Returns true if the current thread is the OGL QueueFlusher thread.
     */
    public static boolean isQueueFlusherThread() {
        return (Thread.currentThread() == getInstance().flusher);
    }

    public void flushNow() {
        // assert lock.isHeldByCurrentThread();
        try {
            flusher.flushNow();
        } catch (Exception e) {
            System.err.println("exception in flushNow:");
            e.printStackTrace();
        }
    }

    public void flushAndInvokeNow(Runnable r) {
        // assert lock.isHeldByCurrentThread();
        try {
            flusher.flushAndInvokeNow(r);
        } catch (Exception e) {
            System.err.println("exception in flushAndInvokeNow:");
            e.printStackTrace();
        }
    }

    private native void flushBuffer(long buf, int limit);

    private void flushBuffer() {
        // assert lock.isHeldByCurrentThread();
        int limit = buf.position();
        if (limit > 0) {
            // process the queue
            flushBuffer(buf.getAddress(), limit);
        }
        // reset the buffer position
        buf.clear();
        // clear the set of references, since we no longer need them
        refSet.clear();
    }

    private class QueueFlusher extends Thread {
        private boolean needsFlush;
        private Runnable task;
        private Error error;

        public QueueFlusher() {
            super("Java2D Queue Flusher");
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
            start();
        }

        public synchronized void flushNow() {
            // wake up the flusher
            needsFlush = true;
            notify();

            // wait for flush to complete
            while (needsFlush) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }

            // re-throw any error that may have occurred during the flush
            if (error != null) {
                throw error;
            }
        }

        public synchronized void flushAndInvokeNow(Runnable task) {
            this.task = task;
            flushNow();
        }

        public synchronized void run() {
            boolean timedOut = false;
            while (true) {
                while (!needsFlush) {
                    try {
                        timedOut = false;
                        /*
                         * Wait until we're woken up with a flushNow() call,
                         * or the timeout period elapses (so that we can
                         * flush the queue periodically).
                         */
                        wait(100);
                        /*
                         * We will automatically flush the queue if the
                         * following conditions apply:
                         *   - the wait() timed out
                         *   - we can lock the queue (without blocking)
                         *   - there is something in the queue to flush
                         * Otherwise, just continue (we'll flush eventually).
                         */
                        if (!needsFlush && (timedOut = tryLock())) {
                            if (buf.position() > 0) {
                                needsFlush = true;
                            } else {
                                unlock();
                            }
                        }
                    } catch (InterruptedException e) {
                    }
                }
                try {
                    // reset the throwable state
                    error = null;
                    // flush the buffer now
                    flushBuffer();
                    // if there's a task, invoke that now as well
                    if (task != null) {
                        task.run();
                    }
                } catch (Error e) {
                    error = e;
                } catch (Exception x) {
                    System.err.println("exception in QueueFlusher:");
                    x.printStackTrace();
                } finally {
                    if (timedOut) {
                        unlock();
                    }
                    task = null;
                    // allow the waiting thread to continue
                    needsFlush = false;
                    notify();
                }
            }
        }
    }
}
