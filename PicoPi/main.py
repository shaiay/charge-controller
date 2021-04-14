from hc05 import HC05
from machine import UART, Pin
import time

uart = UART(
    0,
    baudrate=9600,
    bits=8,
    parity=None,
    stop=1,
    timeout=200,
    tx=Pin(16),
    rx=Pin(17),
)

# wait a bit so that the EN line would not be high at HC-05 boot
time.sleep(1)

bt = HC05(uart, 19)
print(b"BT state: <" + bt.get_state() + b">")




