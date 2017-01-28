#!/bin/sh
RUN=`basename $0`
DIR=`dirname $0`
. ${DIR}/env.sh
echo ${RUN_JAVA_SHUTDOWN}
## uncomment this line for shutdown application
exec  ${RUN_JAVA_SHUTDOWN}
