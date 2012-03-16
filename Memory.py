import os

class Memory:
    def __init__(self):
        self._proc_status = '/proc/%d/status' % os.getpid()
        self._scale = {
            'kB': 1024.0,
            'KB': 1024.0,
            'mB': 1024.0*1024.0,
            'MB': 1024.0*1024.0,
        }

    def _VmB(self, VmKey):
        #global _proc_status, _scale
        try:
            t = open(self._proc_status)
            v = t.read()
            t.close()
        except:
            return 0.0
        i = v.index(VmKey)
        v = v[i:].split(None, 3)  # whitespace
        if len(v) < 3:
            return 0.0
        return float(v[1]) * self._scale[v[2]]

    def memory(self, since=0.0):
        return self._VmB('VmSize:') - since

    def resident(self, since=0.0):
        return self._VmB('VmRSS:') - since

    def stacksize(self, since=0.0):
        return self._VmB('VmStk:') - since

