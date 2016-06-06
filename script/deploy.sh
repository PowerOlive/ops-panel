#!/bin/bash

# XXX: saltify! and do attempt to minimize downtime.

# Warning: No attempt is made to minimize downtime.

set -e

REPO_ROOT=$(dirname "${BASH_SOURCE[0]}")/..
NGINX_ROOT=/opt/nginx-clojure-0.4.4
DATOMIC_ROOT=/opt/datomic-free-0.9.5372

echo "Stopping nginx..."
# Free the memory for the build process.
sudo service nginx stop
echo "Stopping datomic..."
sudo service datomic stop

cd $REPO_ROOT
echo "Building uberjar..."
boot build
echo "Replacing uberjar..."
sudo cp target/project.jar $NGINX_ROOT/libs/ops-panel.jar

echo "Replacing config files..."
sudo cp etc/nginx.conf $NGINX_ROOT/conf/nginx.conf
sudo cp etc/nginx.upstart_conf /etc/init/nginx.conf

sudo cp etc/datomic-transactor.properties $DATOMIC_ROOT/config/datomic-transactor.properties
sudo cp etc/datomic.upstart_conf /etc/init/datomic.conf

echo "Setting owner and permissions..."
sudo chown root:root $NGINX_ROOT/conf/nginx.conf
sudo chown root:root /etc/init/nginx.conf
sudo chown root:root $DATOMIC_ROOT/config/datomic-transactor.properties
sudo chown root:root /etc/init/datomic.conf
sudo chmod 644 $NGINX_ROOT/conf/nginx.conf
sudo chmod 644 /etc/init/nginx.conf
sudo chmod 644 $DATOMIC_ROOT/config/datomic-transactor.properties
sudo chmod 644 /etc/init/datomic.conf

echo "Reloading initctl configuration..."
sudo initctl reload-configuration

echo "Starting datomic..."
sudo service datomic start
echo "Starting nginx..."
sudo service nginx start
cd -
echo "Done."
