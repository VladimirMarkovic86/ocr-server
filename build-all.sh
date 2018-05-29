#!/bin/bash
cd ../mongo_lib
lein install
cd ../ocr_lib
lein install
cd ~/workspace/clojurescript/projects/utils_lib
lein install
cd ~/workspace/clojurescript/projects/ajax_lib
lein install
