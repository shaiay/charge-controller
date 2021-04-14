import utime
from machine import Pin

class HC05:
    def __init__(self, uart, en):
        self.uart = uart
        self.en = Pin(en, Pin.OUT)
        
    def get_state(self):
        resp = self.at_cmd('STATE?')
        try:
            return resp[0].split(b':')[1]
        except IndexError:
            return b''
    
    def at_cmd(self, cmd):
        """
        send AT command, and return response
        :param: cmd - just the command, w/o the AT+ at the begining (e.g. 'version?')
        returns the response split into lines. Last line should have success code (OK or ERROR)
        """
        
        cmd = 'AT' + ('+' + cmd if len(cmd) else '') + '\r\n'
        resp = b''
        try:
            self.en.value(1)
            utime.sleep_ms(100) # give some time to enter AT mode
            self.uart.write(cmd.upper())
            resp = self.get_response()
        finally:
            self.en.value(0)
            
        return resp
        
    def get_response(self):
        resp = b""
        while True:
            nextline = self.uart.readline()
            if nextline is None:
                break
            
            resp = resp + nextline
                  
        return resp.replace(b'\r\n',b'\n').split(b'\n')[:-1]

