K-Core Decomposition for Very Large Graphs
===

This repository contains efficient implementations for computing the k-core decomposition of large graphs. The details of these implementations and their optimizations are described in the following paper. 

*Wissam Khaouid, Marina Barsky, Venkatesh Srinivasan, Alex Thomo:
K-Core Decomposition of Large Networks on a Single PC. PVLDB 9(1): 13-23 (2015)*

There are three implementations provided.

KCoreWG_BZ
------

This is an implementation of the algorithm given in: 
*V. Batagelj and M. Zaversnik. An O(m) algorithm for cores decomposition of networks. CoRR, 2003*
using the WebGraph compression framework of
*P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04*

KCoreWG_M
---

This is an implementation (with optimizations) of the algorithm given in: 
*A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition. Parallel and Distributed Systems, IEEE Trans., 24(2), 2013*
using the WebGraph compression framework of
*P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04*

KCoreGC_M
--

This is an implementation (with optimizations) of the algorithm given in: 
*A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition. Parallel and Distributed Systems, IEEE Trans., 24(2), 2013.*
using GraphChi introduced in:
*Aapo Kyrola, Guy E. Blelloch, Carlos Guestrin:
GraphChi: Large-Scale Graph Computation on Just a PC. OSDI 2012: 31-46*

Remarks
--

*KCoreWG_BZ* and *KCoreWG_M* scale much better than *KCoreGC_M* (see the above VLDB paper for details). 

*KCoreWG_BZ* loads the graph in compressed form in main memory. 
*KCoreWG_M* is a vertex-centric algorithm, and it does not load the graph in main memory. Instead it uses memory-mapped files. 

Input
--

The graphs for *KCoreWG_BZ* and *KCoreWG_M* should be in WebGraph format.  There are three files in this format: 
*<basename>.graph*, *<basename>.properties*, and *<basename>.offsets*

There many available datasets in this format in:
http://law.di.unimi.it/datasets.php

Let us see for an example dataset, *cnr-2000*, in 
http://law.di.unimi.it/webdata/cnr-2000
There you can see the following files available for download.
*cnr-2000.graph	1.2M
cnr-2000.properties	4.0K
cnr-2000-t.graph	920K
cnr-2000-t.properties	4.0K
...*
(you can ignore the rest of the files)

The first two files are for the forward (regular) *cnr-2000* graph. The other two are for the transpose (inverse) graph. If you only need the forward graph, just download: 
*cnr-2000.graph	1.2M
cnr-2000.properties	4.0K*

What's missing is the "offsets" file. This can be easily created by running:

**java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L cnr-2000**

(I'm assuming the "lib" directory with the WebGraph jar files is in the same parent directory as the files you downloaded. If not, change "lib/*" accordingly.)

Edgelist format
--
Now, what if the graph file is in the edgelist format? Well, it is very easy to convert it to WebGraph format. I am assuming the following points. 

1. The graph is unlabeled and the vertices are given by numbers, e.g. 0,1,2,... If there are some vertices "missing", e.g. you don't have a vertex 0 in your file, it's not a problem. WebGraph will create dummy vertices, e.g. 0, that does not have any neighbor. 

2. The edgelist file is TAB separated (not comma separated). 

To convert the edgelist file to WebGraph format do the following steps. 

1. Sort the file, then remove any duplicate edges:
**sort -nk 1 edgelistfile | uniq > edgelistsortedfile**
(If you are on Windows, download *sort.exe* and *uniq.exe* from http://gnuwin32.sourceforge.net/packages/coreutils.htm)

2. Run: 
__java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -1 -g ArcListASCIIGraph dummy basename < edgelistsortedfile__
(This will create basename.graph, basename.offsets, basename.properties.
The basename can be for example simplegraph)

Running KCoreWG_BZ and KCoreWG_M
--
KCoreWG_BZ:
__java -cp "bin:lib/*" KCoreWG_BZ basename__

(Change : to ; if you are on Windows)

KCoreWG_M:
__java -cp "bin:lib/*" KCoreWG_M basename__

The result will be stored in a text file _basename.cores_. The lines of the file are of the form _<vertex-id>:<core-number>_.

Running KCoreGC_M
--
__java -Xmx4g -cp "bin:lib/*" -Dnum_threads=4 KCoreGC_M filename nbrOfShards filetype__

__Example:__ 
java -Xmx4g -cp "bin:lib/*" -Dnum_threads=4 KCoreGC_M ./graphchidata/simplegraph.txt 1 edgelist

Why are we running using a file from subdirectory "graphchidata"? This is because GraphChi produces a lot of internal files for the shards it uses, and those files are created in the same directory that the input file is. In order to keep the main directory clean, we have copied the edgelist file in the "graphchidata" subdirectory.

__Remark.__
GraphChi introduces internally some extra vertices which are not part of the graph. So, be prepared to discard the one or more of the last vertices in the output cores file. The latter is located in the "graphchidata" subdirectory.

__Contact.__
For any question, send email to thomo@uvic.ca

