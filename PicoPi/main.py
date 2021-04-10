from hc05 import HC05
from machine import UART, Pin

bt = HC05(UART(0, baudrate=9600, bits=8, parity=None, stop=1, tx=Pin(16), rx=Pin(17)), 20, 22)
print(bt.send_at_cmd("AT+VERSION"))

