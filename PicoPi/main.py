from hc05 import HC05
from machine import UART, Pin

def pc(cmd=''):
    if len(cmd):
        cmd = '+' + cmd
    ret  = bt.send_at_cmd("AT" + cmd)
    return ret

bt = HC05(UART(0, baudrate=9600, bits=8, parity=None, stop=0, tx=Pin(16), rx=Pin(17)), 20, 22)
print(b"BT state: " + bt.get_state())


