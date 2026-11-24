#!/usr/bin/env bash

BASE_DIR=$(dirname $(readlink -f $0))
aws s3 cp $BASE_DIR/modules/flowable-ui/flowable-ui-app/target/*.war s3://pfe-extid-cloudformation/Software/Flowable/
