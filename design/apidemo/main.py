#!/usr/bin/env python3

import readline
import sys
import traceback

from comm import Comm

try:
    from rich.console import Console
    rich_console = Console()
    def plain_print(*args, **kwargs):
        kwargs.setdefault('highlight', False)
        kwargs.setdefault('emoji', True)
        return rich_console.print(*args, **kwargs)
    print = plain_print
except ImportError:
    print("rich module not found, using plain print instead.")
    rich = None

class CmdSh( object ):
    
    def __init__(self):
        self.comm = Comm()
        
        self._available_commands = []
        for attr in dir(self.comm):
            if callable(getattr(self.comm, attr)) and hasattr(getattr(self.comm, attr), '_external_if_'):
                self._available_commands.append(attr)
        
    def loop(self):
        
        while True:
            try:
                uinput= input("cmdsh> ").split(' ')
            except EOFError:
                print("\nsee you next time..")
                break
            
            except KeyboardInterrupt:
                print("\nkeyboardInterrupt received, exiting cmdsh.")
                break
                        
            cmd = uinput[0]
            if cmd == '':
                continue
            para = uinput[1:]
            
            if cmd in self._available_commands:
                try:
                    func = getattr(self.comm, cmd)
                    self._do(func, para)
                except Exception as e:
                    print(f"[red]error on executing command '{cmd}': {e}[/red]")
                    traceback.print_exc()
                
            elif cmd in ('exit', 'quit'):
                print("bye..")
                break
            elif cmd in ('clear', ):
                # clean screen
                print("\033[H\033[J", end="")
            else:
                print('\n[plum1]  available commands:[/plum1]')
                for f in self._available_commands:
                    print(f"\t[light_goldenrod1]{f}[/light_goldenrod1]")
                print('\t[blue]clear[/blue]')
                print('\t[blue]help[/blue]')
                print('\t[blue]exit[/blue]')
                continue
    
    def _do(self, func, para):
        if para:
            result = func(*para)
        else:
            result = func()
        
        if result is not None:
            print(f'--> [cyan]{result}[/cyan]')


def main():
    cmdsh = CmdSh()
    cmdsh.loop()
    
if __name__ == "__main__":
    # Initialize the command shell
    main()