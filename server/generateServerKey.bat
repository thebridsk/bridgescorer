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
keytool -genkeypair -v -alias exampleca -dname "CN=exampleCA, OU=Example Com, O=Example Company, L=New York, ST=New York, C=US" -keystore exampleca.jks -keypass:env PW -storepass:env PW -keyalg RSA -keysize 4096 -ext KeyUsage:critical="keyCertSign" -ext BasicConstraints:critical="ca:true" -validity 9999

rem # Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
keytool -export -v -alias exampleca -file exampleca.crt -keypass:env PW -storepass:env PW -keystore exampleca.jks -rfc

rem # Create a server certificate, tied to example.com
keytool -genkeypair -v -alias example.com -dname "CN=example.com, OU=Example Com, O=Example Company, L=New York, ST=New York, C=US" -keystore example.com.jks -keypass:env PW -storepass:env PW -keyalg RSA -keysize 4096 -validity 385

rem # Create a certificate signing request for example.com
keytool -certreq -v -alias example.com -keypass:env PW -storepass:env PW -keystore example.com.jks -file example.com.csr

rem # Tell exampleCA to sign the example.com certificate. Note the extension is on the request, not the
rem # original certificate.
rem # Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
keytool -gencert -v -alias exampleca -keypass:env PW -storepass:env PW -keystore exampleca.jks -infile example.com.csr -outfile example.com.crt -ext KeyUsage:critical="digitalSignature,keyEncipherment" -ext EKU="serverAuth" -ext SAN="DNS:example.com" -rfc

rem # Tell example.com.jks it can trust exampleca as a signer.
echo yes >yes
keytool -import -v -alias exampleca -file exampleca.crt -keystore example.com.jks -storetype JKS -storepass:env PW < yes

rem # Import the signed certificate back into example.com.jks 
keytool -import -v -alias example.com -file example.com.crt -keystore example.com.jks -storetype JKS -storepass:env PW

rem # List out the contents of example.com.jks just to confirm it.  
rem # If you are using Play as a TLS termination point, this is the key store you should present as the server.
keytool -list -v -keystore example.com.jks -storepass:env PW

rem # Export example.com's public certificate for use with nginx.
keytool -export -v -alias example.com -file example.com.crt -keypass:env PW -storepass:env PW -keystore example.com.jks -rfc

rem # Create a PKCS#12 keystore containing the public and private keys.
keytool -importkeystore -v -srcalias example.com -srckeystore example.com.jks -srcstoretype jks -srcstorepass:env PW -destkeystore example.com.p12 -destkeypass:env PW -deststorepass:env PW -deststoretype PKCS12

rem # Export the example.com private key for use in nginx.  Note this requires the use of OpenSSL.
call cygwin openssl pkcs12 -nocerts -nodes -passout env:PW -passin env:PW -in example.com.p12 -out example.com.key

rem # Create a JKS keystore that trusts the example CA, with the default password.
keytool -import -v -alias exampleca -file exampleca.crt -keypass:env PW -storepass %tpw% -keystore exampletrust.jks < yes

# List out the details of the store password.
keytool -list -v -keystore exampletrust.jks -storepass %tpw%

