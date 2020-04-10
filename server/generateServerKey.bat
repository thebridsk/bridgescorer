@echo off
setlocal
goto process
:help
echo Syntax: %0 servercertpw truststorepw
echo Where:
echo   servercertpw - the password for the private key
echo   truststorepw - the password for the trust store
echo.
echo Obtained from:
echo   https://lightbend.github.io/ssl-config/CertificateGeneration.html
goto :eof

:process
if ".%~1" == ".-h" goto help
if ".%~1" == ".--help" goto help
if ".%~1" == ".-?" goto help
if ".%~1" == "./?" goto help
if ".%~2" == "." goto help

set PW=%1
set tpw=%2

rem # Create a self signed key pair root CA certificate.
keytool -genkeypair -v -alias exampleca -dname "CN=BridgeScoreKeeperCA, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US" -keystore examplebridgescorekeeperca.jks -keypass:env PW -storepass:env PW -keyalg RSA -keysize 4096 -ext KeyUsage:critical="keyCertSign" -ext BasicConstraints:critical="ca:true" -validity 9999

rem # Export the exampleCA public certificate as examplebridgescorekeeperca.crt so that it can be used in trust stores.
keytool -export -v -alias exampleca -file examplebridgescorekeeperca.crt -keypass:env PW -storepass:env PW -keystore examplebridgescorekeeperca.jks -rfc

rem # Create a server certificate, tied to bridgescorekeeper
keytool -genkeypair -v -alias bridgescorekeeper -dname "CN=BridgeScoreKeeper, OU=BridgeScoreKeeper, O=BridgeScoreKeeper, L=New York, ST=New York, C=US" -keystore examplebridgescorekeeper.jks -keypass:env PW -storepass:env PW -keyalg RSA -keysize 4096 -validity 385

rem # Create a certificate signing request for bridgescorekeeper
keytool -certreq -v -alias bridgescorekeeper -keypass:env PW -storepass:env PW -keystore examplebridgescorekeeper.jks -file examplebridgescorekeeper.csr

rem # Tell exampleCA to sign the bridgescorekeeper certificate. Note the extension is on the request, not the
rem # original certificate.
rem # Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
keytool -gencert -v -alias exampleca -keypass:env PW -storepass:env PW -keystore examplebridgescorekeeperca.jks -infile examplebridgescorekeeper.csr -outfile examplebridgescorekeeper.crt -ext KeyUsage:critical="digitalSignature,keyEncipherment" -ext EKU="serverAuth" -ext SAN="DNS:bridgescorekeeper" -rfc

rem # Tell examplebridgescorekeeper.jks it can trust exampleca as a signer.
echo yes >yes
keytool -import -v -alias exampleca -file examplebridgescorekeeperca.crt -keystore examplebridgescorekeeper.jks -storetype JKS -storepass:env PW < yes

rem # Import the signed certificate back into examplebridgescorekeeper.jks
keytool -import -v -alias bridgescorekeeper -file examplebridgescorekeeper.crt -keystore examplebridgescorekeeper.jks -storetype JKS -storepass:env PW

rem # List out the contents of examplebridgescorekeeper.jks just to confirm it.
rem # If you are using Play as a TLS termination point, this is the key store you should present as the server.
keytool -list -v -keystore examplebridgescorekeeper.jks -storepass:env PW

rem # Export bridgescorekeeper's public certificate for use with nginx.
keytool -export -v -alias bridgescorekeeper -file examplebridgescorekeeper.crt -keypass:env PW -storepass:env PW -keystore examplebridgescorekeeper.jks -rfc

rem # Create a PKCS#12 keystore containing the public and private keys.
keytool -importkeystore -v -srcalias bridgescorekeeper -srckeystore examplebridgescorekeeper.jks -srcstoretype jks -srcstorepass:env PW -destkeystore examplebridgescorekeeper.p12 -destkeypass:env PW -deststorepass:env PW -deststoretype PKCS12

rem # Export the bridgescorekeeper private key for use in nginx.  Note this requires the use of OpenSSL.
bash -c "openssl pkcs12 -nocerts -nodes -passout env:PW -passin env:PW -in examplebridgescorekeeper.p12 -out bridgescorekeeper.key"

rem # Create a JKS keystore that trusts the example CA, with the default password.
keytool -import -v -alias exampleca -file examplebridgescorekeeperca.crt -keypass:env PW -storepass %tpw% -keystore examplebridgescorekeepertrust.jks < yes

rem # List out the details of the store password.
keytool -list -v -keystore examplebridgescorekeepertrust.jks -storepass %tpw%

