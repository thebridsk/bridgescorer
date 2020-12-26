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

# Rest Resources

A Rest rest resource has the following components:

- a versioned case class that can be converted to JSON, this is the protocol class for the resource
- converter to convert case class to/from JSON
- a VersionedInstanceJson object to convert older versions to current version
- a CacheStoreSupport object that provides instance specific information to a store
- an unversioned type and object to latest version protocol class
- a store, with following implementations
  - in memory
  - file
  - zip
  - java resource
- a rest trait that implements the REST operations for the resource.  this provides the routes for the resource.

Adding new rest resource requires the following:

- add protocol class for the resource
- add rest trait for the resource
- in BridgeResources class, add an implicit CacheStoreSupport object for the type
- in BridgeService class, add new store
- in BridgeServiceInMemory class, add new store
- in BridgeServiceFileStoreConverters class, add new VersionedInstanceJson
- in BridgeServiceFileStore class, add new store
- in BridgeServiceZipStore class, add new store
- in Service class
  - add new rest type to restTypes list
  - add new instance of rest trait for new type
  - add route for new rest trait into routeRest
- in com.github.thebridsk.bridge.data package.scala add type
- in JsonSupport
  - add KeyReads and KeyWrites for the key to the resource
  - add Format for resource Id
  - add format for resource
