#!/bin/bash

rm -rf database/data/*
mkdir -p database/data
rm -rf minio_data/*
rm -rf minio_data/.minio*
mkdir -p minio_data/tmdad
rm -rf rabbit_data/*
mkdir -p rabbit_data