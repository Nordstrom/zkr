#!/usr/bin/env bash


# Set CLIENT_JVMFLAGS to something like this:
#export CLIENT_JVMFLAGS="-Djava.security.auth.login.config=/var/private/jaas.conf"

JAVA_BIN=`which java`
if [ -z "$ZKR_PATH" ]; then
    ZKR_BASEDIR=`dirname "$0"`
    ZKR_BASEDIR=`cd "$ZKR_BASEDIR" && pwd`
    ZKR_PATH="$ZKR_BASEDIR/build/libs/zkr-all.jar"
fi

if [ ! -f "$ZKR_PATH" ]; then
    echo "Distribution not found: $ZKR_PATH" >&2
    exit 1
fi

exec $JAVA_BIN $CLIENT_JVMFLAGS -cp $ZKR_PATH zkr.Zkr "$@"
