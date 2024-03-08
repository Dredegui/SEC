#!/usr/bin/env python

import os
import json
import sys
import signal


# Terminal Emulator used to spawn the processes
terminal = "kitty"

# Blockchain node configuration file name
server_configs = [
    "regular_config.json"
]


server_config = server_configs[0]

def quit_handler(*args):
    os.system(f"pkill -i {terminal}")
    sys.exit()


# Compile classes
os.system("mvn clean install")

# Spawn blockchain nodes and clients
with open(f"Service/src/main/resources/{server_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        private_key_path = "privateKeys/rk_" + key['id'] + ".key"
        pid = os.fork()
        if pid == 0:
            if "client" not in key['id']:
                os.system(
                    f"{terminal} sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} {private_key_path} {server_config}' ; sleep 500\"")
            elif key['id'] == "client":
                os.system(
                    f"{terminal} sh -c \"cd Client; mvn exec:java -Dexec.args='{key['id']} {private_key_path}' ; sleep 500\""
                )
            elif key['id'] == "client1":
                os.system(
                    f"{terminal} sh -c \"cd Client; mvn exec:java -Dexec.args='{key['id']} {private_key_path}' ; sleep 500\""
                )
            sys.exit()

signal.signal(signal.SIGINT, quit_handler)

while True:
    print("Type quit to quit")
    command = input(">> ")
    if command.strip() == "quit":
        quit_handler()
