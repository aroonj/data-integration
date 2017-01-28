RUN=`basename $0`
DIR=`dirname $0`
cd ${DIR}; cd ..;
APP_DIR=`pwd`

## Change this jar file
EXE_JAR=camel-db-util-1.0.0-SNAPSHOT.jar

## Standard directory
CONF_DIR=${APP_DIR}/etc
LOGS_DIR=${APP_DIR}/logs
LIB_DIR=${APP_DIR}/lib
EXT_DIR=${APP_DIR}/ext.lib

RUN_JAVA_START="java -Dlog4j.configurationFile=${CONF_DIR}/log4j2.properties -Dlog.dir=${LOGS_DIR} -Dconf.dir=${CONF_DIR} -Dext.dir=${EXT_DIR} -jar ${LIB_DIR}/${EXE_JAR} -f ${CONF_DIR}/application.conf -r"


RUN_JAVA_SHUTDOWN="java -Dlog4j.configurationFile=${CONF_DIR}/log4j2.properties -Dlog.dir=${LOGS_DIR} -Dconf.dir=${CONF_DIR} -Dext.dir=${EXT_DIR} -jar ${LIB_DIR}/${EXE_JAR} -f ${CONF_DIR}/application.conf -s"

#echo ${RUN_JAVA_SHUTDOWN}
