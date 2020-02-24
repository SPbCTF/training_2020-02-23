#!/usr/bin/python3
# -*- coding: utf-8 -*-
import base64
import enum
import re
import subprocess
import sys
import time
import traceback

from mysession import *
from PIL import Image, ImageDraw, ImageFont
import tempfile

import enum
import os
import sys

import requests
import random
import string
# import secrets
import struct

alphabet = string.ascii_lowercase + string.digits


def pp(it):
    print(it.text)
    print(it.status_code)


def gen_str():
    # return ''.join([secrets.choice(alphabet) for i in range(12)])
    res = ''
    for _ in range(12):
        idx = struct.unpack("Q", os.urandom(8))[0] % len(alphabet)
        res += alphabet[idx]
    return res


def is_left_bar_present(it):
    return 'var userConfig={"uid":' in it.text \
           and ("Моя Страница" in it.text or "My Profile" in it.text) \
           and ("Мои Друзья" in it.text or "My Friends" in it.text) \
           and ("Мои Новости" in it.text or "My News" in it.text) \
           and ("Мои Настройки" in it.text or "My Settings" in it.text)


class MySession:
    def __init__(self, host, username_prefix):
        self.s = requests.Session()

        self.host = "http://" + host + ":4343"
        self.username_prefix = username_prefix
        # print('>>>new session', self.username_prefix)
        self.logged_in = False

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        # pass
        # return
        if self.s is not None:
            # print("__exit__ , close()")
            self.s.close()

    def gen_creds(self):
        return ("spbctf_" + self.username_prefix, gen_str())

    def login(self, username, password):
        # if self.logged_in:
        #     self.s.close()
        #     self.s = requests.Session()
        #     print('>>>new session', self.username_prefix, 'login')

        r = self.s.post(self.host + '/account/login', data={'username': username, 'password': password})
        # pp(r)
        ok = r.status_code == 200 and '/' + username in r.text and is_left_bar_present(r)
        if ok:
            self.logged_in = True
            # print(self.username_prefix,' logged in by login')
        return ok

    def createInvite(self):
        r = self.s.post(self.host + "/settings/createInvite?hidden=true")
        if r.status_code == 200 and len(r.text) == 32:
            return r.text
        return None

    def friends(self, username):
        r = self.s.get(self.host + "/" + username + "/friends?ajax=true")
        # print(r.text)
        # print(r.status_code)
        if r.status_code == 200:
            return r.text
        return ''

    def register(self, username, password, fname, lname, email, invite, sid=None):
        data = {
            'username': username,
            'password': password,
            'password2': password,
            'first_name': fname,
            'last_name': lname,
            'email': email,
            'invite': invite,
        }
        if sid is not None:
            data['sid'] = sid
        # print(data)
        r = self.s.post(self.host + '/account/register', data=data)
        # pp(r)
        ok = r.status_code == 200 and '/' + username in r.text and is_left_bar_present(r)

        if ok:
            self.logged_in = True
            # print(self.username_prefix, ' logged in by reg')
        return ok

    def get_profile(self, username):
        res = self.s.get(self.host + "/" + username)
        if res.status_code == 200:
            return res.text
        return ''

    def get_profile_by_id(self, uid):
        res = self.s.get(self.host + "/users/" + uid)
        if res.status_code == 200:
            return res.text
        return ''

    def update_profile(self, fname, lname, about, private, extrafs=None):
        data = {
            'firstName': fname,
            'lastName': lname,
            'about': about,
            'privateProfile': "on" if private else "nope",
        }
        if extrafs is not None:
            for k in extrafs:
                data[k] = extrafs[k]
        r = self.s.post(self.host + '/settings/updateName', data=data)
        # pp(r)
        return r.status_code == 200

    def create_wall_post(self, username, text, fname, private):
        if fname != None:
            with open(fname, 'rb') as f:
                files = {'file': ('flag.txt', f, 'image/png')}
                r = self.s.post(self.host + '/system/upload/postPhoto', files=files)
                if r.status_code != 200:
                    return False
        data = {
            'text': text,
            'private': 'on' if private else 'nope',
        }
        r = self.s.post(self.host + '/' + username + '/createWallPost?_ajax=1', data=data)
        postid = r.headers['X-New-post']

        if r.status_code == 200 and postid is not None:
            # print(postid)
            return postid

    def remove_friend(self, username):
        r = self.s.post(self.host + '/' + username + '/doRemoveFriend')
        return r.status_code == 200

    def send_friend_request(self, username):
        r = self.s.post(self.host + '/' + username + '/doSendFriendRequest')
        return r.status_code == 200

    def accept_friend_request(self, username):
        data = {'accept': 'Accept'}
        r = self.s.post(self.host + '/' + username + '/respondToFriendRequest', data=data)
        return r.status_code == 200

    def get_post_by_id(self, pid):
        r = self.s.get(self.host + '/posts/' + str(pid))
        return r.text

    def get_photo(self, link):
        r = self.s.get(self.host + link)
        if r.status_code == 200:
            return r.content


class Status(enum.Enum):
    OK = 101
    CORRUPT = 102
    MUMBLE = 103
    DOWN = 104
    ERROR = 110

    def __bool__(self):
        return self.value is Status.OK


def quit(code, *args, **kwargs):
    if "file" in kwargs:
        raise NameError("use print() to print to file")

    print(*args, **kwargs)
    assert (type(code) == Status)
    sys.exit(code.value)


# ================

IMAGE_COMPARE_TRESHOLD = 0.98

fontpath = os.path.dirname(os.path.abspath(__file__)) + "/JetBrainsMono-Medium.ttf"

font = ImageFont.truetype(fontpath, 32)


def check(host):
    with MySession(host, 'check1_' + gen_str()) as anonymous, MySession(host, 'check2_' + gen_str()) as s1, MySession(
            host, 'check3_' + gen_str()) as s2:
        if not s1.login('korniltsev', 'qwerty'):
            quit(Status.MUMBLE, 'Tolyan ne smog zaloginitsya')

        invite1 = s1.createInvite()
        invite2 = s1.createInvite()
        if not invite1 or not invite2:
            quit(Status.MUMBLE, 'Tolyan ne smo zainvaiti\' drugana')

        u, p = s1.gen_creds()
        if not s1.register(u, p, u, '-_-', u + '@gov.ua', invite1):
            quit(Status.MUMBLE, 'Drugan Tolyana ne smog zaregat\'sya po invaitu')

        # if not s1.login(u, p):
        #     quit(Status.MUMBLE, 'Друган Толяна не смог залогинитсья после реги')

        invite4 = s1.createInvite()
        if not invite4:
            quit(Status.MUMBLE, 'Drugan tolyana ne smog zainvaitit\' drugana')

        u2, p2 = s2.gen_creds()
        if not s2.register(u2, p2, u2, '-_-', u2 + '@gov.ua', invite4):
            quit(Status.MUMBLE, 'Drugan drugana Tolyana ne smog zaregat\'sya po invaitu')

        if u not in s2.friends(u2):
            quit(Status.MUMBLE, 'Drugani doljni bit\' druz\'yami posle invaita')
        if u2 not in s1.friends(u):
            quit(Status.MUMBLE, 'Drugani doljni bit\' druz\'yami posle invaita')

        # if not s2.login(u2, p2):
        #     quit(Status.MUMBLE, 'Друган другана Толяна не смог залогинитсья после реги')

        privateAbout = base64.b64encode('Eto check zdes\' flaga net '.encode('utf-8')).decode('utf-8')
        if not s2.update_profile(u2, '^_^', privateAbout, True):
            quit(Status.MUMBLE, 'Drugan drugana tolyana ne smog obnovit svoi personalnie dannie')
        profile = anonymous.get_profile(u2)
        if '' == profile or privateAbout in profile:
            quit(Status.MUMBLE, 'Sekretik drugana drugana Tolyana ne bil sohranen v sekrete')
        uids = re.findall('property="og:uid"\s+content="(\d+)"', profile)
        if not uids:
            quit(Status.MUMBLE, 'Kazetsya slomalas open  graph razmetka v anonimke')
        if 'commercialized internet sucks ass' not in profile:
            quit(Status.MUMBLE, 'slomalas razmetka my ass')

        profile = s1.get_profile(u2)
        if '' == profile or privateAbout not in profile:
            quit(Status.MUMBLE, 'Drugan Tolyana ne vidit sekretik drugana drugana tolyana')

        if not s2.update_profile(u2, '^_^', privateAbout, False):
            quit(Status.MUMBLE, 'Drugan drugana tolyana ne smog obnovit svoi personalnie dannie')
        profile = anonymous.get_profile_by_id(uids[0])
        if '' == profile or privateAbout not in profile:
            quit(Status.MUMBLE, 'Publichnaya infa drugana tolyana ne dostupna')

        fakeflag = '[CHECK] FLAG_IS_HERE' + str(random.randint(0, 999999999))
        fakeflag2 = 'totally not a flag  ' + str(random.randint(0, 999999999))
        imgf, img = gen_image(fakeflag)
        p1 = s2.create_wall_post(u2, fakeflag, imgf, True)
        if p1 is None:
            quit(Status.MUMBLE, 'Drugan drugana Tolyana ne smog napisat na stenu')
        p2 = s2.create_wall_post(u2, fakeflag2, None, False)
        if p2 is None:
            quit(Status.MUMBLE, 'Drugan drugana Tolyana ne smog napisat na stenu')

        profile = anonymous.get_profile(u2)
        if '' == profile or fakeflag not in profile or fakeflag2 not in profile:
            quit(Status.MUMBLE, 'Stena publichnogo profilya ne rabotayet')

        if not s2.update_profile(u2, '^_^', privateAbout, True):
            quit(Status.MUMBLE, 'Drugan drugana tolyana ne smog obnovit svoi personalnie dannie')

        profile = anonymous.get_profile(u2)
        if '' == profile or fakeflag in profile or fakeflag2 not in profile:
            quit(Status.MUMBLE, 'Stena privatnogo profilya ne rabotayet')

        ok = s2.remove_friend(u)
        u2friends = s2.friends(u2)
        if not ok or u in u2friends:
            quit(Status.MUMBLE, 'Ne udalos udalit druga')

        s3 = anonymous
        u3, p3 = s3.gen_creds()
        if not s3.register(u3, p3, u3, '-_-', u3 + '@gov.ua', invite2):
            quit(Status.MUMBLE, 'Drugan tolyana ne smog zaregatsya po invaitu')
        profile = s3.get_profile(u2)
        if '' == profile or fakeflag in profile or fakeflag2 not in profile:
            quit(Status.MUMBLE, 'Stena privatnogo profile ne rabotaet')

        if not s3.send_friend_request(u2):
            quit(Status.MUMBLE, 'Ne udalos zafrendit druga')

        if not s2.accept_friend_request(u3):
            quit(Status.MUMBLE, 'ne udalos primyat druga')
        if u2 not in s3.friends(u3):
            quit(Status.MUMBLE, 'drujba ne rabotayet')

        profile = s3.get_profile(u2)
        if '' == profile or fakeflag not in profile or fakeflag2 not in profile:
            quit(Status.MUMBLE, 'Stena privatnogo profilya ne rabotayet')

        pic = get_post_image(s3, p1)
        if not compare_image(imgf, pic, IMAGE_COMPARE_TRESHOLD):
            quit(Status.MUMBLE, 'Kartinochka v poste ne soderjit nujnoi infi')

        quit(Status.OK)


def get_post_image(session: MySession, postid):
    links = re.findall("/s/post_media/[^.]+.webp", session.get_post_by_id(postid))
    if not links:
        quit(Status.MUMBLE, "V poste net kartinochki")
    link = links[1] if len(links) >= 2 else links[0]
    # print (link)
    pic = session.get_photo(link)
    if not pic:
        quit(Status.MUMBLE, "Ne udalos skachat kartinochku iz posta")
    return pic


def gen_image(text):
    t1 = time.time()
    f = tempfile.mktemp('check.webp')
    img = Image.new('RGB', (700, 96), color=(255, 255, 255))
    d = ImageDraw.Draw(img)

    d.text((32, 32), text, fill=(22, 22, 22), font=font)
    img.save(f)
    t2 = time.time()
    # print(t2-t1, "gne)img")
    return f, img


def compare_image(img1, img2bs, treshold):
    # temp = tempfile.mktemp('temppost.webp')
    # temp_dif = tempfile.mktemp('temppost_diff.webp')

    with open(img1, 'rb') as f:
        return f.read() == img2bs
    # print(img1, temp, temp_dif)

    # p = subprocess.Popen(['magick', 'compare', '-metric', 'NCC', img1, temp, temp_dif], stderr=subprocess.PIPE)
    # ec = p.wait()
    #
    # communicate = p.communicate()
    # # print(communicate,  ec)
    # if ec == 0 or float(communicate[1]) >= treshold:
    #     return True
    # else:
    #     return False


def gen_debug_flag():
    return ''.join([random.choice(string.ascii_uppercase) for _ in range(31)]) + '='


def put(host, flag_id, flag, vuln):
    vuln = int(vuln)
    assert vuln == 2 or vuln == 1
    with MySession(host, 'put1_' + flag_id) as s1, MySession(host, 'put2_' + flag_id) as s2:
        if not s1.login('korniltsev', 'qwerty'):
            quit(Status.MUMBLE, 'Tolyan ne smog zaloginitsya')

        invite = s1.createInvite()
        if not invite:
            quit(Status.MUMBLE, 'Tolyan ne smog zainvaitit drugana')

        u, p = s1.gen_creds()
        if not s1.register(u, p, u, '-_-', u + '@gov.ua', invite):
            quit(Status.MUMBLE, 'Drugan Tolyana ne smog zaregatsya po invaitu')

        invite2 = s1.createInvite()
        if not invite2:
            quit(Status.MUMBLE, 'Drugan Tolyana ne smog zainvaitit drugana')

        u2, p2 = s2.gen_creds()
        if not s2.register(u2, p2, u2, '-_-', u2 + '@gov.ua', invite2):
            quit(Status.MUMBLE, 'Drugan drugana tolyana ne smog zaregatsya')

        if vuln == 1:
            if not s2.update_profile(u2, '^_^', flag, True):
                quit(Status.MUMBLE, 'Drugan drugana tolyana ne smog obnovit svoi personalnie dannie')
            print('\n'.join([u, u2, p2, ]))
            quit(Status.OK)
        else:
            if not s2.update_profile(u2, '^_^', '', True):
                quit(Status.MUMBLE, 'Drugan drugana tolyana ne smog obnovit svoi personalnie dannie')
            fpath, img = gen_image(flag)
            postid = s2.create_wall_post(u2, 'FLAG_IS_HERE', fpath, True)
            if not postid:
                quit(Status.MUMBLE, 'Drugan drugana tolyana ne smog sozdat post s kartinkoi')
            print('\n'.join([u, u2, p2, postid, fpath]))
            quit(Status.OK)


# def get(url, flag, bugtype, username, password, postid=None, filepath=None):
def get(host, flag_id, flag, vuln):
    vuln = int(vuln)
    ids = flag_id.split('\n')
    username1 = ids[0]
    username2 = ids[1]
    password2 = ids[2]
    assert vuln == 1 or vuln == 2
    with MySession(host, '$%4%45$') as s1:
        friends_tolyan = s1.friends('korniltsev')
        friends_u1 = s1.friends(username1)
        if username1 not in friends_tolyan:
            quit(Status.CORRUPT, 'Drugana tolyana net v spiske druzei')
        if username2 not in friends_u1:
            quit(Status.CORRUPT, 'Drugana drugana tolyana net v spiske druzei')

    with MySession(host, '$%4%45$') as s1:
        if not s1.login(username2, password2):
            quit(Status.MUMBLE, 'Drugan drugana Tolyana ne smog zaloginitsya')
        profile = s1.get_profile(username2)
        if vuln == 1:
            if flag not in profile:
                quit(Status.MUMBLE, "Sekretik drugana drugana tolyana ne bil sohranen")
            else:
                quit(Status.OK)
        else:
            postid = ids[3]
            filepath = ids[4]
            pic = get_post_image(s1, postid)
            if not compare_image(filepath, pic, IMAGE_COMPARE_TRESHOLD):
                quit(Status.MUMBLE, 'kartinochka v poste drugana drugana tolyana ne bila sohranena')
            else:
                quit(Status.OK)


VULNS = "1:1"


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
            quit(Status.OK)
        else:
            print("Unknown action:", action)
            quit(Status.ERROR)

        quit(Status.ERROR, 'Action handler has not quit correctly')
    except requests.exceptions.ConnectionError as e:
        quit(Status.DOWN, 'ConnectionError')
    except requests.exceptions.Timeout as e:
        quit(Status.DOWN, 'timeout')
    except Exception as e:
        traceback.print_exc()
        quit(Status.ERROR)


if __name__ == "__main__":
    main()




