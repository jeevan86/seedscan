try:
    import hashlib
    sha1 = hashlib.sha1
except:
    import sha
    sha1 = sha.new

import base64
import psycopg2
import sys

class Database:
    def __init__(self, conString=None):
        self.db = None
        self.cur = None
        if conString:
            self.select_database(conString)

    def __del__(self):
        self.close()

# ===== Public Methods ===========================
    def select_database(self, conString):
        self.close()
        host, user, pwd, db, port = conString.split(',')
        self.db = psycopg2.connect(host=host, user=user, password=pwd, database=db, port=port)
        self.cur = self.db.cursor()

    def close(self):
        if self.cur:
            self.cur.close()
            del self.cur
        if self.db:
            self.db.close()
            del self.db

    def execute(self, query, data=None):
        if data is not None:
            self.cur.execute(query, data)
        else:
            self.cur.execute(query)

    def select(self, query, data=None):
        if data is not None:
            self.cur.execute(query, data)
        else:
            self.cur.execute(query)
        return self.cur.fetchall()

    def insert(self, query, data=None, commit=True):
        if data is not None:
            self.cur.execute(query, data)
        else:
            self.cur.execute(query)
        if commit:
            self.db.commit()

    def insert_many(self, query, iterator, commit=True):
        print query
	#print iterator
	self.cur.executemany(query, iterator)
        if commit:
            self.db.commit()

    def delete(self, query, commit=True):
        self.cur.execute(query)
        if commit:
            self.db.commit()

    def interrupt(self):
        self.db.interrupt()

    def run_script(self, script):
        return self.cur.executescript(script)

    def commit(self):
        self.db.commit()

# ===== Private Methods ==========================
    def _hash(self, text):
        sha_obj = sha1()
        sha_obj.update(text)
        return base64.urlsafe_b64encode(sha_obj.digest())

