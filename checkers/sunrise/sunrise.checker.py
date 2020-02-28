#!/usr/bin/env python

from socket import *
import sys,string,random,re

def randomString(stringLength=10):
    """Generate a random string of fixed length """
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for i in range(stringLength))

cmd = sys.argv[1]
PORT = 3771
TIMEOUT = 10

def readall(s):
    mes=''
    try:
        while 1:
            m = s.recv(1)
            mes += m.decode('utf-8')
    except timeout:
        return mes

def readuntil(s,term):
    mes=''
    i=0
    while 1:
        m = s.recv(1)
        #print(m)
        mes += m.decode('utf-8')
        i+=1
        if m.decode('utf-8') == term or i>64:
            break;
    return mes

s = socket(AF_INET,SOCK_STREAM)
s.settimeout(TIMEOUT)

if cmd == 'check':
    try :
        IP = sys.argv[2]
        s.connect((IP,PORT))
        ss = readuntil(s,':')
        if "Welcome to the Sunrise service. Enter command:" in ss:
            s.close()
            exit(101)
        s.close()
        exit(102)
    except Exception as e:
        sys.stderr.write(str(e))
        exit(102)
elif cmd == 'put':
    try:
        IP = sys.argv[2]
        s.connect((IP,PORT))
        ss = readuntil(s,":")
        s.send("put\n".encode())
        flag_id = sys.argv[3]
        flag = sys.argv[4]
        ss = readuntil(s,":")
        s.send(flag_id.encode()+b"\n")
        passwd = randomString(5)
        ss = readuntil(s,":")
        s.send(passwd.encode()+b"\n")
        ss = readuntil(s,":")
        s.send(flag.encode()+b"\n")
        ss = readuntil(s,"\n")

        s.send("search\n".encode())
        ss = readuntil(s,"\n")
        s.send((flag_id[:3]+"\n").encode())
        aa=s.recv(1024)
        if not flag_id in aa.decode():
            sys.stderr.write("No search")
            exit(102)
        print(",".join([flag_id,passwd]))
        s.close()
        exit(101)
    except Exception as e:
        sys.stderr.write(str(e))
        exit(102)
elif cmd == 'get':
    try:
        IP = sys.argv[2]
        s.connect((IP,PORT))
        ss = readuntil(s,":")
        s.send("get\n".encode())
        flag_id,passwd = sys.argv[3].split(",")
        flag = sys.argv[4]
        ss = readuntil(s,":")
        s.send(flag_id.encode()+b"\n")
        ss = readuntil(s,":")
        s.send(passwd.encode()+b"\n")
        ss = readuntil(s,":")
        flg = s.recv(1024)
        if flag in flg.decode('utf-8'):
            exit(101)
        exit(102)
    except Exception as e:
        sys.stderr.write(str(e))
        exit(102)
elif cmd == 'info':
    print("vulns: 1")
