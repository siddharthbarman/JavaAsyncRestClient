# Using flask to make an api import necessary libraries and functions
from flask import Flask, jsonify, request
from datetime import datetime
import socket
import json
import hashlib
import sys
import os
import time

app = Flask(__name__)
started_at = datetime.now()
cmd_args = sys.argv
version = sys.argv[1]

@app.route('/', methods = ['GET'])
def home():
    if(request.method == 'GET'):
        data = { "service-start": started_at, "service-version": version }
        time.sleep(1)
        return jsonify(data)

@app.route('/n1', methods = ['GET'])
def getN1():
    #time.sleep(1)
    return "1";
    
@app.route('/n2', methods = ['GET'])
def getN2():
    #time.sleep(2)
    return "2";

if __name__ == '__main__':
    print(cmd_args)
    for k, v in sorted(os.environ.items()):
        print(k, "=", v)
    app.run(host ='0.0.0.0', port = 5000, debug = False)