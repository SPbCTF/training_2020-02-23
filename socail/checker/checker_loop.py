import os
import random
import string
import time
import subprocess

from mysession import gen_str


def gen_debug_flag():
    return ''.join([random.choice(string.ascii_uppercase) for _ in range(31)]) + '='


host = '6.6.6.2'

def interact(args):
    p = subprocess.Popen(args, stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    communicate = p.communicate()
    ec = p.wait()
    print(ec, communicate)
    return (ec, communicate[0])

while True:
    ec, out = interact(["/usr/local/bin/python3", 'checker.py', 'check', host])

    ec, out = interact(["/usr/local/bin/python3", 'checker.py', 'info'])

    flagid = gen_str()
    flag = gen_debug_flag()
    ec, out = interact(["/usr/local/bin/python3", 'checker.py', 'put', host, flagid, flag, "1"])
    flagid = out
    ec, out = interact(["/usr/local/bin/python3", 'checker.py', 'get', host, flagid, flag, "1"])

    flagid = gen_str()
    flag = gen_debug_flag()
    ec, out = interact(["/usr/local/bin/python3", 'checker.py', 'put', host, flagid, flag, "2"])
    flagid = out
    ec, out = interact(["/usr/local/bin/python3", 'checker.py', 'get', host, flagid, flag, "2"])


    break
