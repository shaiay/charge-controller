import time


class Commands:
    def __init__(self, switch):
        self._switch = switch
        self._callable_commands = self._get_commands()
        print(self._callable_commands)
        
    def __call__(self, command):
        for c in self._callable_commands:
            if c.startswith(command):
                return getattr(self, c)(command)
        return b'error - unknow command\r\n'
    
    def _get_commands(self):
        return [
            m
            for m in dir(self)
            if not m.startswith('_')
        ]
        
    def ping(self, cmd):
        return b"pong\r\n"

    def on(self, cmd):
        self._switch.on()
        return b"on\r\n"

    def off(self, cmd):
        self._switch.off()
        return b"off\r\n"
    

def main_loop(bt, switch):
    commands = Commands(switch)
    while True:
        if bt.uart.any():
            cmd = bt.uart.readline().strip()
            print(cmd)
            ret = commands(cmd)
            print(ret)
            bt.uart.write(ret)
               
        print("Waiting ...")
        time.sleep(1)

