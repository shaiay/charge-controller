import time


class Commands:
    def __init__(self, switch):
        self.switch = switch
        
    def get_commands(self):
        return [
            m
            for m in dir(self)
            if not m.startswith('_') and callable(getattr(self, m))
        ]
        
    def ping(self, cmd):
        return b"pong\r\n"

    def on(self, cmd):
        self.switch.on()
        return b"on\r\n"

    def off(self, cmd):
        self.switch.off()
        return b"off\r\n"
    

def main_loop(bt, switch):
    commands = Commands(switch)
    available_commands = commands.get_commands()
    while True:
        if bt.uart.any():
            cmd = bt.uart.readline().strip()
            for c in available_commands:
                if c.startswith(cmd.lower()):
                    print(cmd)
                    ret = getattr(commands, c)(cmd)
                    print(ret)
                    bt.uart.write(ret)
                    break
               
        print("Waiting ...")
        time.sleep(1)

