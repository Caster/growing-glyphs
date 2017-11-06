# Initial running time
This is the output of the first run on clustering Trove (`trove-dump-uniq.tsv`).

    INFO    |  created 8,335,897 events, handled 8,118 and discarded 7,896,507; 431,272 events were never considered
    INFO    |  took 432,318 ms (wall clock time)

Plan of action:

  - try to reduce time it takes to load file (not in above output, but it is considerable);
  - focus on determining as fast as possible whether an event should be discarded;
  - more general profiling.
