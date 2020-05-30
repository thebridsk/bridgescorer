# BridgeScorer Server

## Generating SSL keys

Run the below commands in an sbt shell.

To get help:

    bridgescorer-server/run sslkey --help
    bridgescorer-server/run sslkey <cmd> --help


To generate both the CA and server certificates.  One long line split for readability.

    bridgescorer-server/run
        sslkey
        -d key
        generateselfsigned
        -a bsk
        --ca examplebridgescorekeeperca
        --caalias ca
        --cadname "CN=BridgeScoreKeeperCA, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US"
        --cakeypw abcdef
        --castorepw abcdef
        --dname "CN=BridgeScoreKeeper, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US"
        --keypass abcdef
        --server examplebridgescorekeeper
        --storepass abcdef
        --trustpw abcdef
        --truststore examplebridgescorekeepertrust
        -v
        --nginx


To generate just the server certificate

    bridgescorer-server/run
        sslkey
        -d key
        generateservercert
        -a bsk
        --ca examplebridgescorekeeperca
        --caalias ca
        --cakeypw abcdef
        --castorepw abcdef
        --dname "CN=BridgeScoreKeeper, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US"
        --keypass abcdef
        --server examplebridgescorekeeper
        --storepass abcdef
        --trustpw abcdef
        --truststore examplebridgescorekeepertrust
        -v
        --nginx

To generate just the server certificate, adding IP

    bridgescorer-server/run
        sslkey
        -d key
        generateservercert
        -a bsk
        --ca examplebridgescorekeeperca
        --caalias ca
        --cakeypw abcdef
        --castorepw abcdef
        --keypass abcdef
        --server examplebridgescorekeeper
        --storepass abcdef
        --trustpw abcdef
        --truststore examplebridgescorekeepertrust
        -v
        --nginx
        --addmachineip

To generate the CA certificate

    bridgescorer-server/run
        sslkey
        -d key
        generateca
        --ca examplebridgescorekeeperca
        --caalias ca
        --cadname "CN=BridgeScoreKeeperCA, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US"
        --cakeypw abcdef
        --castorepw abcdef
        --trustpw abcdef
        --truststore examplebridgescorekeepertrust
        -v
        --clean

To validate the server certificate, this checks the dates on the certificate chain and the machine IP

    bridgescorer-server/run
        --logfile ../server/logs/server.sbt.%d.%u.log
        sslkey
        validatecert
        -a bsk
        -k examplebridgescorekeeper.jks
        -p abcdef
        -s
