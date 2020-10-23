#!/bin/bash

psql -v ON_ERROR_STOP=1 << --eosql--
create database metricscollector ;
create user metricscollector with encrypted password 'metricscollector';
grant all privileges on database metricscollector to metricscollector;

--eosql--
