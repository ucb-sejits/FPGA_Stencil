from __future__ import print_function

__author__ = 'Chick Markley chick@eecs.berkeley.edu U.C. Berkeley'


import numpy as np

a = np.array([1.0, -1.0, 23.77, 3.14e7], dtype=np.float32)
print("a {}".format(a))

b = a.tobytes()
print("b {}".format(b))

with open('xxx', 'wb') as f:
    f.write(b)
f.close()

with open('xxx', 'rb') as g:
    c = g.read()
g.close()
print("c {}".format(c))

d = np.fromstring(c, dtype=np.float32)
print("d {}".format(d))
