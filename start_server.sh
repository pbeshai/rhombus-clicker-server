#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd "$DIR"
java -Djava.library.path=lib -jar ClickerServer.jar $@

