K-Core Decomposition of Very Large Graphs
===

This repository contains efficient implementations for computing the k-core decomposition of large graphs. The details of the implementations and their optimizations are described in the following paper: 

*Wissam Khaouid, Marina Barsky, Venkatesh Srinivasan, Alex Thomo:
K-Core Decomposition of Large Networks on a Single PC. PVLDB 9(1): 13-23 (2015)*

There are three implementations provided.

KCoreWG_BZ
------

This is an implementation of the algorithm in: 

*V. Batagelj and M. Zaversnik. An O(m) algorithm for cores decomposition of networks. CoRR, 2003*

using the WebGraph compression framework in:

*P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04*

KCoreWG_M
---

This is an implementation (with optimizations) of the algorithm in:

*A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition. Parallel and Distributed Systems, IEEE Trans., 24(2), 2013*

using the WebGraph compression in:

*P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04*

KCoreGC_M
--

This is an implementation (with optimizations) of the algorithm in: 

*A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition. Parallel and Distributed Systems, IEEE Trans., 24(2), 2013.*

using the GraphChi framework in:

*Aapo Kyrola, Guy E. Blelloch, Carlos Guestrin:
GraphChi: Large-Scale Graph Computation on Just a PC. OSDI 2012: 31-46*


Remarks
--

*KCoreWG_BZ* loads the graph in compressed form in main memory. Because of the superb compression that WebGraph provides, we can fit in the main memory of a moderate machine with 12 GB RAM a large graph, such as Twitter 2010, which has about 1.5 billion edges (http://law.di.unimi.it/webdata/twitter-2010/). 

*KCoreWG_M* is a vertex-centric algorithm, and it does not load the graph in main memory. Instead, it uses memory-mapped files. As such, it can scale to much bigger graphs, e.g. Clueweb 2012, which has more than 42 billion edges
(http://law.di.unimi.it/webdata/clueweb12/).



Compiling
--
The programs are already compiled with javac 1.8 (Java 8). If your java is older, you can compile as follows:

__javac -cp "lib/\*" -d bin src/*__


Input for KCoreWG_BZ and KCoreWG_M
--

The graphs for *KCoreWG_BZ* and *KCoreWG_M* should be in WebGraph format.  

There are three files in this format: 

*basename.graph* <br>
*basename.properties* <br>
*basename.offsets*

(see simplegraph example in the main directory)

There many available datasets in this format in:
http://law.di.unimi.it/datasets.php

Let us see for an example dataset, *cnr-2000*, in 
http://law.di.unimi.it/webdata/cnr-2000

There you can see the following files available for download.

*cnr-2000.graph* <br>
*cnr-2000.properties* <br>
*cnr-2000-t.graph* <br>
*cnr-2000-t.properties* <br>
*...* <br>
(you can ignore the rest of the files)

The first two files are for the forward (regular) *cnr-2000* graph. The other two are for the transpose (inverse) graph. If you only need the forward graph, just download: 

*cnr-2000.graph* <br>
*cnr-2000.properties*

What's missing is the "offsets" file. This can be easily created by running:

__java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L cnr-2000__


Edgelist format
--
This section is for the case when your graph is given a text file of edges (known as edgelist). *If your graph is already in WebGraph format, skip to the next section.* 

It is very easy to convert an edgelist file into WebGraph format. 
I am making the folloiwng assumptions: 

1. The graph is unlabeled and the vertices are given by consecutive numbers, 0,1,2,... <br> (If there are some vertices "missing", e.g. you don't have a vertex 0 in your file, it's not a problem. WebGraph will create dummy vertices, e.g. 0, that does not have any neighbor.) 

2. The edgelist file is TAB separated (not comma separated). 

Now, to convert the edgelist file to WebGraph format execute the following steps: 

Sort the file, then remove any duplicate edges:

**sort -nk 1 edgelistfile | uniq > edgelistsortedfile**

(If you are on Windows, download *sort.exe* and *uniq.exe* from http://gnuwin32.sourceforge.net/packages/coreutils.htm)

Run: 

__java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -1 -g ArcListASCIIGraph dummy basename &lt; edgelistsortedfile__

(This will create *basename.graph, basename.offsets, basename.properties*.
The basename can be e.g. __simplegraph__)



Running KCoreWG_BZ and KCoreWG_M
--
KCoreWG_BZ:

__java -cp "bin:lib/*" KCoreWG_BZ basename__

e.g. <br> java -cp "bin:lib/*" KCoreWG_BZ simplegraph

(Change : to ; if you are on Windows)

KCoreWG_M:

__java -cp "bin:lib/*" KCoreWG_M basename__

e.g. <br> java -cp "bin:lib/*" KCoreWG_M simplegraph

The result will be stored in a text file _basename.cores_. The lines of the file are of the form _vertex-id:core-number_.


Undirected graphs in WebGraph
--
While our implementations can work with directed graphs by considering the *outdegree* of the vertices as their *degree*, the definition of k-core is often given for undirected graphs. 

In order to obtain undirected graphs, for each edge we add its inverse. This can be achieved by taking the union of the graph with its *transpose*. Here we show how to do this for cnr-2000.

Download from http://law.di.unimi.it/datasets.php:

*cnr-2000.graph* <br>
*cnr-2000.properties* <br>
*cnr-2000-t.graph* <br>
*cnr-2000-t.properties*

(The last two files are for the transpose graph.)

Build the offsets:

__java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L cnr-2000__ <br>
__java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L cnr-2000-t__

Symmetrize by taking union:

__java -cp "lib/*" it.unimi.dsi.webgraph.Transform union cnr-2000 cnr-2000-t cnr-2000-sym__





GraphChi: Running KCoreGC_M
--
For GraphChi the input needs to be an edgelist (not WebGraph format). GraphChi does not read WebGraph format. To run, execute:

__java -Xmx4g -cp "bin:lib/*" -Dnum_threads=4 KCoreGC_M filename nbrOfShards filetype__

__Example:__ 
java -Xmx4g -cp "bin:lib/*" -Dnum_threads=4 KCoreGC_M ./graphchidata/simplegraph.txt 1 edgelist

Why are we running using a file from subdirectory "graphchidata"? This is because GraphChi produces a lot of internal files for the shards it creates and those files are placed in the same directory that the input file is. In order to keep the main directory clean, we have copied the edgelist file in the "graphchidata" subdirectory.

__Remark.__
GraphChi introduces internally some extra vertices which are not part of the graph. So, you might need to discard one (or more) of the last vertices in the output cores file. The latter is located in the *"graphchidata"* subdirectory.

Contact
--

For any question, send email to thomo@uvic.ca
