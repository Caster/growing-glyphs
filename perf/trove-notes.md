# Initial running time
This is the output of the first run on clustering Trove (`trove-dump-uniq.tsv`).

    INFO    |  created 8,335,897 events, handled 8,118 and discarded 7,896,507; 431,272 events were never considered
    INFO    |  took 432,318 ms (wall clock time)

## Plan of action

  - Try to reduce time it takes to load file (not in above output, but it is considerable);
  - focus on determining as fast as possible whether an event should be discarded;
  - more general profiling.

# After adding more logging

    FINE    |  ENTRY into CsvIO#read()
    INFO    |  counting lines took 6,147 ms (wall clock time)
    INFO    |  constructing HashMap took 0 ms (wall clock time)
    FINE    |  RETURN from CsvIO#read()
    INFO    |  reading file took 42,971 ms (wall clock time)
    INFO    |  read 15,772,243 entries and ignored 438,657

# After parallelizing reading file with a Stream

...no improvement. It appears that using streams is not as easy as I thought it would be.

## Plan of action

Implement custom thread pool and properly parallellize file loading.

## Updated plan of action

Parallelizing reading the file appears to only make things slower. Giving up on that.

    INFO    |  reading file took 35,798 ms (wall clock time)
    INFO    |  read 15,772,243 entries and ignored 438,657
    INFO    |  created 8,335,893 events, handled 8,118 and discarded 7,896,507; 431,268 events were never considered
    INFO    |  clustering took 415,210 ms (wall clock time)

# Parallellize clustering?

General idea: process events within a cell of the QuadTree. As soon as an OUT_OF_CELL event is generated, pause. This gives a partial clustering and a set of alive glyphs. Four of these partial clusterings can be combined to get the partial clustering of the parent of four QuadTree cells. That would require a possibility to "roll back" a partial clustering, so that the clustering can continue from the earliest point in time of the four partial clusterings. QuadTree cells can track whether they have been clustered already.

Overall approach: have a queue with "work packages". Each package is either a cell and some glyphs (initial state), or four partially clustered cells and the assignment to combine them. When clustering of a cell has completed, it should be checked if this was the last cell of its siblings to complete. If so, the parent should be added to the queue as a work package. Clustering completes when the root has been clustered. Because work packages are disjoint, this can be parallellized.

The hope is that not too much work needs to be undone when combining partial clusterings.

# More debug information

Now logging more detailed logging information: specific operations, nanosecond accuracy, number of events per type, queue size, ...

## Trove

    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 5.909 nodes and height 15
    FINE    |  created 39.439 events initially, for 7.754 glyphs
    FINE    |  created 8.335.893 events, handled 8.118 and discarded 7.896.508; 431.267 events were never considered
    FINE    |  created 340.814 out of cell events (365 handled, 235.931 discarded)
    FINE    |  created 7.995.079 merge events (7.753 handled, 7.660.577 discarded)
    FINE    |  clustering took 409,628 seconds (wall clock time)
    FINE    |  queue operations took 11,18 seconds (wall clock time, 16.240.519 timings)
    FINE    |  queue size was 1.887.522,031 on average and always between 1 and 2.855.186, over 16.240.519 measurements
    FINE    |  glyphs per cell was 1.593,515 on average and always between 567 and 2.412, over 4.432 measurements

Clearly, the QuadTree needs to be subdivided adaptively while the glyphs are growing.

## Risse (used for debugging + optimizing)

    INFO    |  loaded 447 locations
    INFO    |  read 6.924 entries and ignored 0
    FINE    |  QuadTree has 341 nodes and height 9
    FINE    |  created 2.246 events initially, for 447 glyphs
    FINE    |  created 27.972 events, handled 2.647 and discarded 23.232; 2.093 events were never considered
    FINE    |  → 18.942 out of cell events (2.201 handled, 15.061 discarded)
    FINE    |  → 9.030 merge events (446 handled, 8.171 discarded)
    FINE    |  QuadTree has 341 nodes and height 9 now
    FINE    |  clustering took 0,392 seconds (wall clock time)
    FINE    |  queue operations took 0,021 seconds (wall clock time, 53.851 timings)
    FINE    |  queue size was 8.148,914 on average and always between 1 and 13.036, over 53.851 measurements
    FINE    |  glyphs per cell was 137,625 on average and always between 0 and 295, over 256 measurements

As one can see, the logging format changed a bit while working on the implementation. Adaptive subdivision resulted in the below result.

    INFO    |  loaded 447 locations
    INFO    |  read 6.924 entries and ignored 0
    FINE    |  QuadTree has 341 nodes and height 9
    FINE    |  created 2.246 events initially, for 447 glyphs
    FINE    |  created 25.643 events, handled 3.220 and discarded 20.342; 2.081 events were never considered
    FINE    |  → 17.660 out of cell events (2.774 handled, 13.211 discarded)
    FINE    |  → 7.983 merge events (446 handled, 7.131 discarded)
    FINE    |  QuadTree has 629 nodes and height 11 now
    FINE    |  clustering took 0,569 seconds (wall clock time)
    FINE    |  queue operations took 0,021 seconds (wall clock time, 49.205 timings)
    FINE    |  queue size was 6.983,72 on average and always between 1 and 10.834, over 49.205 measurements
    FINE    |  glyphs per cell was 0,511 on average and always between 0 and 5, over 544 measurements

    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 5.909 nodes and height 15, having at most 5 glyphs per cell and cell size at least 0,001
    FINE    |  created 39.439 events initially, for 7.754 glyphs
    FINE    |  created 8.528.420 events, handled 8.121 and discarded 8.080.127; 440.172 events were never considered
    FINE    |  → 341.260 out of cell events (368 handled, 236.552 discarded)
    FINE    |  → 8.187.160 merge events (7.753 handled, 7.843.575 discarded)
    FINE    |  QuadTree has 6.277 nodes and height 15 now
    FINE    |  clustering took 460,273 seconds (wall clock time)
    FINE    |  queue operations took 12,694 seconds (wall clock time, 16.616.668 timings)
    FINE    |  queue size was 1.918.751,305 on average and always between 1 and 2.900.394, over 16.616.668 measurements
    FINE    |  glyphs per cell was 0,17 on average and always between 0 and 5, over 4.793 measurements

Seems to have gotten a bit slower, but changing the maximum number of glyphs per cell does have a big impact on performance.

# Finding sweet spot for maximum number of glyphs per cell

## at most 50 glyphs/cell: 57,811 seconds
    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 657 nodes and height 11, having at most 50 glyphs per cell and cell size at least 0,001
    FINE    |  created 135.500 events initially, for 7.754 glyphs
    FINE    |  created 6.846.394 events, handled 7.957 and discarded 6.261.882; 576.555 events were never considered
    FINE    |  → 168.456 out of cell events (204 handled, 94.420 discarded)
    FINE    |  → 6.677.938 merge events (7.753 handled, 6.167.462 discarded)
    FINE    |  QuadTree has 689 nodes and height 11 now
    FINE    |  clustering took 57,811 seconds (wall clock time)
    FINE    |  queue operations took 10,027 seconds (wall clock time, 13.116.233 timings)
    FINE    |  queue size was 1.892.456,943 on average and always between 1 and 2.856.039, over 13.116.233 measurements
    FINE    |  glyphs per cell was 0,43 on average and always between 0 and 22, over 525 measurements

## at most 100 glyphs/cell: 39,494 seconds
    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 333 nodes and height 11, having at most 100 glyphs per cell and cell size at least 0,001
    FINE    |  created 234.806 events initially, for 7.754 glyphs
    FINE    |  created 7.047.984 events, handled 7.819 and discarded 6.272.350; 767.815 events were never considered
    FINE    |  → 136.430 out of cell events (66 handled, 70.376 discarded)
    FINE    |  → 6.911.554 merge events (7.753 handled, 6.201.974 discarded)
    FINE    |  QuadTree has 361 nodes and height 11 now
    FINE    |  clustering took 39,494 seconds (wall clock time)
    FINE    |  queue operations took 10,24 seconds (wall clock time, 13.328.153 timings)
    FINE    |  queue size was 2.086.314,241 on average and always between 1 and 3.060.897, over 13.328.153 measurements
    FINE    |  glyphs per cell was 0,932 on average and always between 0 and 26, over 278 measurements

## at most 200 glyphs/cell: 29,485 seconds
    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 185 nodes and height 8, having at most 200 glyphs per cell and cell size at least 0,001
    FINE    |  created 436.398 events initially, for 7.754 glyphs
    FINE    |  created 7.594.051 events, handled 7.823 and discarded 6.359.518; 1.226.710 events were never considered
    FINE    |  → 118.308 out of cell events (70 handled, 52.954 discarded)
    FINE    |  → 7.475.743 merge events (7.753 handled, 6.306.564 discarded)
    FINE    |  QuadTree has 209 nodes and height 8 now
    FINE    |  clustering took 29,485 seconds (wall clock time)
    FINE    |  queue operations took 9,888 seconds (wall clock time, 13.961.392 timings)
    FINE    |  queue size was 2.490.642,093 on average and always between 1 and 3.498.958, over 13.961.392 measurements
    FINE    |  glyphs per cell was 4,196 on average and always between 0 and 123, over 163 measurements

## at most 500 glyphs/cell: 27,989 seconds
    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 81 nodes and height 6, having at most 500 glyphs per cell and cell size at least 0,001
    FINE    |  created 1.092.365 events initially, for 7.754 glyphs
    FINE    |  created 9.128.530 events, handled 7.851 and discarded 6.581.706; 2.538.973 events were never considered
    FINE    |  → 96.174 out of cell events (98 handled, 35.634 discarded)
    FINE    |  → 9.032.356 merge events (7.753 handled, 6.546.072 discarded)
    FINE    |  QuadTree has 81 nodes and height 6 now
    FINE    |  clustering took 27,989 seconds (wall clock time)
    FINE    |  queue operations took 10,667 seconds (wall clock time, 15.718.087 timings)
    FINE    |  queue size was 3.582.380,938 on average and always between 1 and 4.800.366, over 15.718.087 measurements
    FINE    |  glyphs per cell was 0 on average and always between 0 and 0, over 61 measurements

# Fewer merge events: big speedup! 6,463 seconds

    FINE    |  QuadTree has 81 nodes and height 6, having at most 500 glyphs per cell and cell size at least 0,001
    FINE    |  created 37.203 events initially, for 7.754 glyphs
    FINE    |  created 111.636 events, handled 7.851 and discarded 37.853; 65.932 events were never considered
    FINE    |  → 96.174 out of cell events (98 handled, 35.634 discarded)
    FINE    |  → 15.462 merge events (7.753 handled, 2.219 discarded)
    FINE    |  QuadTree has 81 nodes and height 6 now
    FINE    |  clustering took 6,463 seconds (wall clock time)
    FINE    |  queue operations took 0,053 seconds (wall clock time, 157.340 timings)
    FINE    |  queue size was 60.557,506 on average and always between 1 and 96.935, over 157.340 measurements
    FINE    |  glyphs per cell was 0 on average and always between 0 and 0, over 61 measurements

## at most 10.000 glyphs/cell (so, no QuadTree really...): 2,984 seconds

    FINE    |  QuadTree has 1 nodes and height 0, having at most 10.000 glyphs per cell and cell size at least 0,001
    FINE    |  created 7.753 events initially, for 7.754 glyphs
    FINE    |  created 15.505 events, handled 7.753 and discarded 3.247; 4.505 events were never considered
    FINE    |  → 0 out of cell events (0 handled, 0 discarded)
    FINE    |  → 15.505 merge events (7.753 handled, 3.247 discarded)
    FINE    |  QuadTree has 1 nodes and height 0 now
    FINE    |  clustering took 2,984 seconds (wall clock time)
    FINE    |  queue operations took 0,012 seconds (wall clock time, 26.505 timings)
    FINE    |  queue size was 5.610,292 on average and always between 1 and 7.753, over 26.505 measurements
    FINE    |  glyphs per cell was 0 on average and always between 0 and 0, over 1 measurements
