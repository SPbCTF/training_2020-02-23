#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import enum
import traceback
import requests
import string
import random
import socket
import struct
import time
import re


class Status(enum.Enum):
    OK = 101
    CORRUPT = 102
    MUMBLE = 103
    DOWN = 104
    ERROR = 110

    def __bool__(self):
        return self.value is Status.OK


__std_print = print
VULNS = "1:3"
PORT = '40000'
TOKENS = {
    '127.0.0.1': 'asd',
}


def print(*args, **kwargs):
    if "file" in kwargs:
        __std_print(*args, **kwargs)
    else:
        raise NameError("print() without file= should not be used")


def log(*args, **kwargs):
    if "file" in kwargs:
        raise NameError("use print() to print to file")

    __std_print(*args, **kwargs, file=sys.stderr)


def rand_str(l=10, alp=string.ascii_letters + string.digits):
    return ''.join([random.choice(alp) for _ in range(l)])


def quit(code, *args, **kwargs):
    if "file" in kwargs:
        raise NameError("use print() to print to file")

    __std_print(*args, **kwargs)
    assert (type(code) == Status)
    sys.exit(code.value)


def register_user(host, sess, username, password):
    try:
        sess.headers['User-Agent'] = br_name
        resp = sess.post('http://{}:{}/register'.format(host, PORT), timeout=5, data={
            'name': username,
            'password': password,
        })

        return resp

    except requests.exceptions.ConnectionError:
        quit(Status.DOWN, "Can't register user")


def login_user(host, sess, username, password):
    try:
        sess.headers['User-Agent'] = br_name
        resp = sess.post('http://{}:{}/login'.format(host, PORT), timeout=5, data={
            'name': username,
            'password': password,
        })

        return resp

    except requests.exceptions.ConnectionError:
        quit(Status.DOWN, "Can't login user")


def register_service(host, sess, service_name, address, port, meta):
    try:
        sess.headers['User-Agent'] = br_name
        resp = sess.post('http://{}:{}/v1/register'.format(host, PORT), timeout=5, data={
            'service': service_name,
            'address': address,
            'port': port,
            'meta': meta,
        })

        return resp

    except requests.exceptions.ConnectionError:
        quit(Status.DOWN, "Can't register service")


def deregister_service(host, sess, service_name):
    try:
        sess.headers['User-Agent'] = br_name
        resp = sess.post('http://{}:{}/v1/deregister'.format(host, PORT), timeout=5, data={
            'service': service_name,
        })

        return resp

    except requests.exceptions.ConnectionError:
        quit(Status.DOWN, "Can't deregister service")


def health_pass(host, sess, service_name):
    try:
        sess.headers['User-Agent'] = br_name
        resp = sess.post('http://{}:{}/v1/health/pass'.format(host, PORT), timeout=5, data={
            'service': service_name,
        })

        return resp

    except requests.exceptions.ConnectionError:
        quit(Status.DOWN, "Can't healthcheck pass service")


def health_fail(host, sess, service_name):
    try:
        sess.headers['User-Agent'] = br_name
        resp = sess.post('http://{}:{}/v1/health/fail'.format(host, PORT), timeout=5, data={
            'service': service_name,
        })

        return resp

    except requests.exceptions.ConnectionError:
        quit(Status.DOWN, "Can't healthcheck fail service")


def check_register(host):
    username = rand_str(20)
    password = rand_str(20)

    sess = requests.Session()
    register_user(host, sess, username, password)
    d = register_user(host, sess, username, password)
    if d.status_code != 400:
        quit(Status.MUMBLE, "Can register one user twice")


br_name = rand_str(10)
headers = {
    'User-Agent': br_name
}


def get_metric(host):
    try:
        resp = requests.get('http://{}:{}/metrics'.format(host, PORT), timeout=5, headers=headers)

        try:
            c = re.search(r'promhttp_metric_handler_requests_total{code="200"} (\d+)', resp.text).group(1)
            return int(c)
        except IndexError:
            quit(Status.MUMBLE, "Can't find my metrics")
        except Exception:
            quit(Status.MUMBLE, "Can't parse my metric")

    except requests.exceptions.ConnectionError:
        quit(Status.DOWN, "Can't get /metrics")


def check(host):
    # Check metrics

    try:
        c = get_metric(host)
        c2 = get_metric(host)
        if c2 <= c:
            quit(Status.MUMBLE, "Invalid metrics on /metrics")

        try:
            resp = requests.get('http://{}:{}/'.format(host, PORT), allow_redirects=False, timeout=5, headers=headers)

            if resp.status_code != 303 or resp.headers['Location'] != '/login':
                quit(Status.MUMBLE, "No redirect on /")

            try:
                upid = resp.cookies['upid']
                if upid != '1' and upid != '2':
                    quit(Status.MUMBLE, "upstream id not found")
            except KeyError:
                quit(Status.MUMBLE, "upid cookie not set")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        check_register(host)

        sess = requests.Session()
        try:
            sess.headers['User-Agent'] = br_name
            sess.get('http://{}:{}/'.format(host, PORT), timeout=5)

            try:
                upid = sess.cookies['upid']
                if upid != '1' and upid != '2':
                    quit(Status.MUMBLE, "upstream id not found")
            except KeyError:
                quit(Status.MUMBLE, "upid cookie not set")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        username = rand_str(18)
        password = rand_str(18)

        resp = register_user(host, sess, username, password)
        if resp.status_code != 200:
            log(resp)
            quit(Status.MUMBLE, "Can't register user")

        resp = login_user(host, sess, username, password)
        if resp.status_code != 200:
            log(resp)
            quit(Status.MUMBLE, "Can't login user")

        try:
            sess.cookies['upid'] = '1'
            index = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)
            if '@{} : 1'.format(username) not in index.text:
                quit(Status.MUMBLE, "Can't get username and qouta on /")

            if 'version: 1' not in index.text:
                quit(Status.MUMBLE, "Can't retrieve version on /")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        try:
            sess.cookies['upid'] = '2'
            index = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)
            if '@{} : 1'.format(username) not in index.text:
                quit(Status.MUMBLE, "Can't get username and qouta on /")

            if 'version: 2' not in index.text:
                quit(Status.MUMBLE, "Can't retrieve version on /")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        sess.cookies['upid'] = random.choice(['1', '2'])

        service = rand_str(10, string.ascii_letters + '-')
        address = socket.inet_ntoa(struct.pack('>I', random.randint(1, 0xffffffff)))
        port = str(random.randint(1, 65535))
        meta = rand_str(50)

        resp = register_service(host, sess, service, address, port, meta)
        if resp.status_code != 200:
            quit(Status.MUMBLE, "Can't register service")

        time.sleep(5)

        try:
            index = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)
            if '@{}'.format(username) not in index.text:
                quit(Status.MUMBLE, "Can't get username on /")

            if 'version: 1' not in index.text and 'version: 2' not in index.text:
                quit(Status.MUMBLE, "Can't retrieve version on /")

            if service not in index.text or meta not in index.text:
                quit(Status.MUMBLE, "Can't obtain my service on /")

            if '<span class="badge badge-danger">Fail</span>' not in index.text:
                quit(Status.MUMBLE, "Can't obtain healthcheck of my service")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        try:
            index = sess.get('http://{}:{}/services'.format(host, PORT), timeout=5)
            if '@{}'.format(username) not in index.text:
                quit(Status.MUMBLE, "Can't get username on /services")

            if 'version: 1' not in index.text and 'version: 2' not in index.text:
                quit(Status.MUMBLE, "Can't retrieve version on /services")

            if service not in index.text:
                quit(Status.MUMBLE, "Can't obtain my service on /services")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        # Check healthchecks
        resp = health_pass(host, sess, service)
        if resp.status_code != 200:
            quit(Status.MUMBLE, "Can't healthcheck pass")

        try:
            index = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)

            if '<span class="badge badge-success">OK</span>' not in index.text:
                quit(Status.MUMBLE, "Can't obtain healthcheck of my service")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        resp = health_fail(host, sess, service)
        if resp.status_code != 200:
            quit(Status.MUMBLE, "Can't healthcheck fail")

        try:
            index = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)

            if '<span class="badge badge-danger">Fail</span>' not in index.text:
                quit(Status.MUMBLE, "Can't obtain healthcheck of my service")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        # Check KV
        k = rand_str()
        v = rand_str()

        try:

            resp = requests.get('http://{}:{}/kv/set?key={}&value={}'.format(host, PORT, k, v), timeout=5,
                                headers=headers)

            if resp.status_code != 200 or resp.text != 'OK':
                quit(Status.MUMBLE, "Can't set entry in KV")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        try:
            resp = requests.get('http://{}:{}/kv/get?key={}'.format(host, PORT, k), timeout=5, headers=headers)

            if resp.status_code != 200 or resp.text != v:
                quit(Status.MUMBLE, "Can't get entry in KV")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        # Check buy
        sess2 = requests.Session()
        sess2.headers['User-Agent'] = br_name
        username2 = rand_str(18)
        password2 = rand_str(18)

        register_user(host, sess2, username2, password2)

        try:
            resp = sess2.post('http://{}:{}/v1/buy'.format(host, PORT), timeout=5, data={
                'service': service,
                'token': TOKENS[host],
            })

            if resp.status_code != 200 or resp.text != meta:
                quit(Status.MUMBLE, "Can't buy service info")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't buy service info")

        # Check deregister service
        deregister_service(host, sess2, service)
        try:
            index = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)
            if service not in index.text or meta not in index.text:
                quit(Status.MUMBLE, "Can deregister not my service!")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        deregister_service(host, sess, service)
        try:
            index = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)
            if service in index.text or meta in index.text:
                quit(Status.MUMBLE, "Can't deregister my service")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        quit(Status.OK)
    except requests.exceptions.ReadTimeout:
        quit(Status.DOWN, "Read timeout")


def put(host, flag_id, flag, vuln):
    if int(vuln) == 1:
        try:
            resp = requests.get('http://{}:{}/kv/set?key={}&value={}'.format(host, PORT, flag_id, flag),
                                timeout=5, headers=headers)

            if resp.status_code != 200 or resp.text != 'OK':
                quit(Status.MUMBLE, "Can't set entry in KV")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /kv/set")

        quit(Status.OK)

    if int(vuln) == 2:
        username = rand_str(18)
        password = rand_str(18)

        sess = requests.Session()
        sess.headers['User-Agent'] = br_name
        resp = register_user(host, sess, username, password)
        if resp.status_code != 200:
            quit(Status.MUMBLE, "Can't register user")

        address = socket.inet_ntoa(struct.pack('>I', random.randint(1, 0xffffffff)))
        port = str(random.randint(1, 65535))

        resp = register_service(host, sess, flag_id, address, port, flag)
        if resp.status_code != 200:
            quit(Status.MUMBLE, "Can't register service")

        quit(Status.OK, "{}:{}".format(username, password))


def get(host, flag_id, flag, vuln):
    if int(vuln) == 1:
        try:
            resp = requests.get('http://{}:{}/kv/get?key={}'.format(host, PORT, flag_id),
                                timeout=5, headers=headers)

            if resp.status_code != 200:
                quit(Status.MUMBLE, "Can't get entry from KV")

            if resp.text != flag:
                quit(Status.CORRUPT, "Got invalid flag from KV")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /kv/get")

        quit(Status.OK)

    if int(vuln) == 2:
        username, password = flag_id.split(':')

        sess = requests.Session()
        sess.headers['User-Agent'] = br_name
        resp = login_user(host, sess, username, password)
        if resp.status_code != 200:
            quit(Status.MUMBLE, "Can't login user")

        try:
            resp = sess.get('http://{}:{}/'.format(host, PORT), timeout=5)
            if '<td>{}</td>'.format(flag) not in resp.text:
                quit(Status.CORRUPT, "Flag not found on /")

        except requests.exceptions.ConnectionError:
            quit(Status.DOWN, "Can't get /")

        quit(Status.OK)


def main():
    script, action, *args = sys.argv
    try:
        if action == "info":
            quit(Status.OK, "vulns:", VULNS)
        elif action == "check":
            host, = args
            check(host)
        elif action == "put":
            host, flag_id, flag, vuln = args
            put(host, flag_id, flag, vuln)
        elif action == "get":
            host, flag_id, flag, vuln = args
            get(host, flag_id, flag, vuln)
        else:
            log("Unknown action:", action)
            quit(Status.ERROR)

        log("Action handler has not quit correctly")
        quit(Status.ERROR)
    except SystemError as e:
        raise
    except Exception as e:
        traceback.print_exc()
        quit(Status.ERROR)


if __name__ == "__main__":
    main()
