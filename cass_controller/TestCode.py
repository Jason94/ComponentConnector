__author__ = 'Matt'

import os, sys

lib_path = os.path.abspath(os.path.join('..', 'int_code_test'))
sys.path.append(lib_path)

lib_path = os.path.abspath(os.path.join('..', 'set_up'))
sys.path.append(lib_path)

import IntCass
import IntCode
import SetUp

testint = IntCass.IntKeyspace(0,['10.104.251.45'])

#for i in testint.intquery('SELECT * FROM cmptable'):
#    print i.ints

#test = IntCode.IntCode([1,2,3,4])

#nhbs = test.neighborsgen()

#for i in nhbs:
#    print i

test = testint.intquery("SELECT * FROM cmptable WHERE uid = '09999'")

for i in test:
    print i.ints
#for i in test:
#    for j in testint.getnhbs(i):
#        for k in j:
#            print k.ints

