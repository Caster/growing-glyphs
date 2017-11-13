package algorithm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.Glyph;
import datastructure.events.Event;
import datastructure.events.GlyphMerge;
import datastructure.growfunction.GrowFunction;

public class FirstMergeRecorder {

    /**
     * Glyph with which merges are recorded.
     */
    private Glyph from;
    /**
     * Function to determine when merges occur.
     */
    private GrowFunction g;
    /**
     * First time at which a merge event with {@link #from} is recorded so far.
     */
    private double minAt;
    /**
     * Set of glyphs that touch {@link #from} at time {@link #minAt}. In
     * practice this will almost always contain just a single glyph.
     */
    private Set<Glyph> minGlyphs;
    /**
     * Input for {@link #workers}. Refer to {@link Worker} for details.
     */
    private Glyph[] tasks;
    /**
     * Workers that are used for parallelizing recording.
     */
    private Worker[] workers;
    /**
     * Pool of threads that workers will run on.
     */
    private ExecutorService workerPool;
    /**
     * Service wrapping {@link #workerPool} that is used to run many jobs in a
     * single threadpool, and easily wait for tasks to complete.
     */
    private ExecutorCompletionService<Double> workerService;


    /**
     * Construct a recorder that will use the given {@link GrowFunction} to
     * determine when glyphs should merge.
     */
    public FirstMergeRecorder(GrowFunction g) {
        this.from = null;
        this.g = g;
        this.minAt = Double.MAX_VALUE;
        this.minGlyphs = new HashSet<>(1);

        this.tasks = null;
        int numParallel = Math.max(1,
                Runtime.getRuntime().availableProcessors() / 2);
        if (numParallel > 1) {
            this.workers = new Worker[numParallel];
            for (int i = 0; i < numParallel; ++i) {
                this.workers[i] = new Worker(i);
            }
            this.workerPool = Executors.newFixedThreadPool(numParallel);
            this.workerService = new ExecutorCompletionService<>(this.workerPool);
        } else {
            this.workers = null;
            this.workerPool = null;
            this.workerService = null;
        }
    }


    /**
     * @see #addEventsTo(Queue, Logger)
     */
    public void addEventsTo(Queue<Event> q) {
        addEventsTo(q, null);
    }

    /**
     * Given the glyph {@link #from} which recording started, and all possible
     * merges that have been {@link #record(Glyph) recorded} after that, one or
     * more merge events will occur first; those are added to the given queue by
     * this method. State is maintained, although it is recommended that this is
     * not used, only {@link #from} could be used to reset state and start over.
     *
     * @param q Queue to add merge events to.
     * @param l Logger to log events to, can be {@code null}.
     */
    public void addEventsTo(Queue<Event> q, Logger l) {
        for (Glyph with : minGlyphs) {
            q.add(new GlyphMerge(from, with, minAt));
            if (l != null) {
                l.log(Level.FINEST, "-> merge at {0} with {1}",
                        new Object[] {minAt, with});
            }
        }
    }

    /**
     * Start recording possible merges with the given glyph, forgetting about
     * all previous state.
     *
     * @param from Glyph with which merges should be recorded starting now.
     */
    public void from(Glyph from) {
        this.from = from;
        this.minAt = Double.MAX_VALUE;
        this.minGlyphs.clear();
        for (int i = 0; i < workers.length; ++i) {
            this.workers[i].reset();
        }
    }

    /**
     * Record a possible merge event.
     *
     * @param candidate Glyph that the {@code from} glyph could merge with.
     */
    public void record(Glyph candidate) {
        double at = g.intersectAt(from, candidate);
        if (at < minAt) {
            minAt = at;
            minGlyphs.clear();
            minGlyphs.add(candidate);
        } else if (at == minAt) {
            minGlyphs.add(candidate);
        }
    }

    /**
     * {@link #record(Glyph) Record} all glyphs in the given array between the
     * given indices (including {@code from}, excluding {@code upto}). No further
     * filtering is done!
     *
     * This method may use parallelization to speed up recording.
     *
     * @param glyphs Array of glyphs to look in.
     * @param from First index of glyph to record.
     * @param upto Index up to but excluding which glyphs will be recorded.
     */
    public void record(Glyph[] glyphs, int from, int upto) {
        // when there are enough tasks, do this in parallel
        if (workers != null && (upto - from) / workers.length > 1000) {
            // find tasks
            tasks = Arrays.copyOfRange(glyphs, from, upto);
            // submit workers that will start working on the tasks
            for (int i = 0; i < workers.length; ++i) {
                workerService.submit(workers[i]);
            }
            // wait until workers are done
            for (int i = 0; i < workers.length; ++i) {
                try {
                    workerService.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
            // merge the results
            for (int i = 0; i < workers.length; ++i) {
                Worker w = workers[i];
                if (w.minAt < minAt) {
                    minAt = w.minAt;
                    minGlyphs.clear();
                    minGlyphs.addAll(w.minGlyphs);
                } else if (w.minAt == minAt) {
                    minGlyphs.addAll(w.minGlyphs);
                }
            }
        } else {
            // when there are few tasks, the overhead of using threads is not
            // worth it, so do everything in a sequential fashion
            for (int i = from; i < upto; ++i) {
                record(glyphs[i]);
            }
        }
    }

    /**
     * {@link #record(Glyph) Record} all glyphs in the given set, as long as
     * they are {@link Glyph#alive} and not {@link #from}.
     *
     * This method may use parallelization to speed up recording.
     *
     * @param glyphs Set of glyphs to record.
     */
    public void record(Set<Glyph> glyphs) {
        Glyph[] arr = glyphs.parallelStream()
                .filter((glyph) -> (glyph.alive && glyph != from))
                .toArray(Glyph[]::new);
        record(arr, 0, arr.length);
    }

    /**
     * Stop background threads. When this is done, no further recording tasks
     * can be accepted. No errors are thrown when they are.
     */
    public void shutdown() {
        if (workerPool != null) {
            workerPool.shutdown();
        }
    }


    /**
     * Same functionality, but bundled in a worker for parallelization purposes.
     *
     * A worker always uses the {@link FirstMergeRecorder#from} and
     * {@link FirstMergeRecorder#g grow function} of its parent.
     */
    private class Worker implements Callable<Double> {

        private int id;
        private double minAt;
        private Set<Glyph> minGlyphs;

        public Worker(int id) {
            this.id = id;
            this.minAt = Double.MAX_VALUE;
            this.minGlyphs = new HashSet<>(1);
        }

        @Override
        public Double call() throws Exception {
            int tasksPerWorker = (int) Math.round(((double) tasks.length) / workers.length);
            int from = tasksPerWorker * id;
            int upto = tasksPerWorker * (id + 1);
            if (id == workers.length - 1) {
                upto = tasks.length;
            }

            for (int i = from; i < upto; ++i) {
                Glyph glyph = tasks[i];
                double at = g.intersectAt(FirstMergeRecorder.this.from, glyph);
                if (at < minAt) {
                    minAt = at;
                    minGlyphs.clear();
                    minGlyphs.add(glyph);
                } else if (at == minAt) {
                    minGlyphs.add(glyph);
                }
            }
            return minAt;
        }

        public void reset() {
            minAt = Double.MAX_VALUE;
            minGlyphs.clear();
        }

    }

}
