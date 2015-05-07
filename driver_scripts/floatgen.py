import random

NUM_FLUSH = 50
NUM_FLOAT = 100
FLOAT_LOW = -1000
FLOAT_HIGH = 1000

#Flush with zeros
for i in xrange(NUM_FLUSH):
	print(0.0)

#Generate random floats
for j in xrange(NUM_FLOAT):
	print(random.uniform(FLOAT_LOW, FLOAT_HIGH))

#Flush again with zeros
for k in xrange(NUM_FLUSH):
	print(0.0)