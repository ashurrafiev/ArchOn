builder = PyNocBuilder(size*2, size+1)

for x in range(size*2):
    for y in range(size+1):
        if y==0:
            builder.set(x, y, NodeType.memory)
        elif y%2==1:
            builder.set(x, y, NodeType.onlyRouter)
        else:
            builder.set(x, y, NodeType.coreA15cache)
