#!/bin/sh
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
python -m black ${SCRIPT_DIR}/mx_trufflesom.py
python -m pylint ${SCRIPT_DIR}/mx_trufflesom.py
