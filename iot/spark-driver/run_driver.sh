#!/bin/sh

if [ ! -z "$SPARK_MASTER_HOST" ] && [ ! -z "$SPARK_MASTER_PORT" ]; then
    export SPARK_MASTER=spark://$SPARK_MASTER_HOST:$SPARK_MASTER_PORT
elif [ -z "$SPARK_MASTER_SERVICE_HOST" ]; then
    export SPARK_MASTER=local[*]
else
    export SPARK_MASTER=spark://$SPARK_MASTER_SERVICE_HOST:$SPARK_MASTER_SERVICE_PORT_SPARK
fi

if [ -z "$MESSAGING_SERVICE_HOST" ] || [ -z "$MESSAGING_SERVICE_PORT" ] || [ -z "$MESSAGING_USERNAME" ] || [ -z "$MESSAGING_PASSWORD" ] || [ -z "$MESSAGING_CERT" ]; then
    CRED_DIR="/etc/app-credentials"
    echo "Looking for messaging info through $CRED_DIR"

    if [ -f "$CRED_DIR/messagingHost" ]; then
        export MESSAGING_SERVICE_HOST=`cat $CRED_DIR/messagingHost`
    else
        echo "Missing $CRED_DIR/messagingHost"
    fi

    if [ -f "$CRED_DIR/messagingAmqpPort" ]; then
        # TODO: TLS support
        export MESSAGING_SERVICE_PORT=`cat $CRED_DIR/messagingAmqpPort`
    else
        echo "Missing $CRED_DIR/messagingAmqpPort"
    fi

    if [ -f "$CRED_DIR/username" ]; then
        export MESSAGING_USERNAME=`cat $CRED_DIR/username`
    else
        echo "Missing $CRED_DIR/username"
    fi

    if [ -f "$CRED_DIR/password" ]; then
        export MESSAGING_PASSWORD=`cat $CRED_DIR/password`
    else
        echo "Missing $CRED_DIR/password"
    fi
else
    echo "Using settings from environment"
fi

echo "Spark master node = " $SPARK_MASTER

${SPARK_HOME}/bin/spark-submit \
    --class io.enmasse.iot.TemperatureAnalyzer \
    --name ${SPARK_APP} \
    --master $SPARK_MASTER \
    /spark-driver-1.0-SNAPSHOT.jar
