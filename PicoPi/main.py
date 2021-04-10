from hc05 import HC05
from machine import UART, Pin

def pc(cmd):
    print(bt.send_at_cmd("AT+" + cmd))

bt = HC05(UART(0, baudrate=9600, bits=8, parity=None, stop=0, tx=Pin(16), rx=Pin(17)), 20, 22)
pc('state?')
# print(bt.send_at_cmd("AT+VERSION?", 10000))

