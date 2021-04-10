import utime
from machine import Pin

class HC05:
    def __init__(self, uart, state, en):
        self.uart = uart
        self.state = Pin(state, Pin.IN)
        self.en = Pin(en, Pin.OUT)
        
    def send_at_cmd(self, cmd, timeout=2000):
        orig_en = self.en.value()
        self.en.on()
        self.uart.write(cmd.upper() + '\r\n', timeout)
        resp = self.wait_ok(timeout)
        self.en.on() if orig_en else self.en.off()
        return resp
        
    def wait_ok(self, timeout=2000):
        start_time = utime.ticks_ms()
        resp = b""
        while utime.ticks_ms() - start_time < timeout:
            if self.uart.any():
                resp = b"".join([resp, self.uart.read(1)])
                if resp[-4:] == b'OK\r\n':
                    break
        return resp

