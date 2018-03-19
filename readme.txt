
=== Bus GUI demo ===

Run BusDemo.bat or from command line:
java -cp bin ncl.cs.prime.archon.HiCoreDemo

Click node to select it.
Click left panel to change the selected node's type. '+' removes the node.

Hot keys:
Ctrl-Z	undo last action
Delete	remove the selected node
Backspace	remove the selected node
Ctrl-S	write ArchOn script to busbuilder.sim file


=== NoC GUI demo ===

Run NocDemo.bat or from command line:
java -cp bin ncl.cs.prime.archon.HiCoreDemo -noc

Click left panel to select "active" node type.
Click or drag on the NoC to paint with active node type.
Click '+' to add new row or column, or both and fill it with active node type.
Click or drag triangles to select rows/columns.

Hot keys:
Space	fill selected rows/columns with active node type
Delete	remove selected rows/columns
Backspace	remove selected rows/columns
F1 or 1	make NoC type A (the current number of cores will be used)
F2 or 2	make NoC type B
F3 or 3	make NoC type C
F4 or 4	make NoC type D
Ctrl-S	write ArchOn script to nocbuilder.sim file


=== Many-core simulation ===

From command line, run:
java -cp bin ncl.cs.prime.archon.HiCoreSim <parameters>
or:
Sim.bat <parameters>

Parameters:
-m <int>	cache miss rate %
-min <int>	start iteration (Iteration i defines the number of simulated cores n. For NoC, n is usually the square of i)
-max <int>	end iteration
-w <int>	total workload (instructions). This number will be evenly split between the cores, unless -nosplit is specified
-nosplit		do not split the workload between cores
-bus		simulate platform with a bus (homogeneous, one level of cache)
-noca		simulate NoC type A
-nocb		simulate NoC type B
-nocc		simulate NoC type C
-nocd		simulate NoC type D

=== On Mac/Linux ===

You can rename *.bat files to *.sh files. However, to make them runnable, you also need to do:
chmod 755 *.sh
