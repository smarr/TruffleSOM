#!/bin/sh
python -m black mx_trufflesom.py
python -m pylint mx_trufflesom.py
