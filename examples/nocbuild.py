w = 8
h = 5
builder = PyNocBuilder(w, h)

for x in range(w):
    for y in range(h):
        if y==0:
            builder.set(x, y, NodeType.memory)
        elif y%2==1:
            builder.set(x, y, NodeType.onlyRouter)
        else:
            builder.set(x, y, NodeType.coreA15cache)
