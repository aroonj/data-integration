#!/bin/sh
RUN=`basename $0`
DIR=`dirname $0`
. ${DIR}/env.sh
echo ${RUN_JAVA_START}
## uncomment this line for run application
exec  ${RUN_JAVA_START}
