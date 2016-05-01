#!/usr/bin/env bash


mvn -DskipTests=true clean install

DB=batch-psql
RMQ=batch-rabbitmq

cf d -f partition-worker
cf d -f partition-master

cf s | grep $DB || cf cs elephantsql panda $DB
cf s | grep $RMQ || cf cs cloudamqp tiger $RMQ

# TODO: u may want to now run `cf env` and get the MySQL credentials and then
# TODO: cat ddl/schema-init.sql | mysql && cat ddl/data-init.sql | mysql

cf push -f manifest-worker.yml
cf push -f manifest-master.yml