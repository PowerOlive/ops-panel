#!/bin/bash

set -e

REPO_ROOT=$(dirname "${BASH_SOURCE[0]}")
NGINX_ROOT=/opt/nginx-clojure-0.4.4

cd $REPO_ROOT
boot build
cp target/project.jar $NGINX_ROOT/libs/ops-panel.jar
cp etc/nginx.conf $NGINX_ROOT/conf/nginx.conf
service nginx stop
cp etc/upstart_conf /etc/init/nginx.conf
initctl reload-configuration
service nginx start
cd -
