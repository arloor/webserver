mkdir cert
cd cert
openssl req -x509 -newkey rsa:4096 -sha256 -nodes -keyout privkey.pem -out cert.pem -days 3650 -subj "/C=/ST=/L=/O=/OU=/CN=example.com"
openssl pkcs12 -export -inkey privkey.pem -in cert.pem -password pass:"123456" -out cert.p12 -name myalias
cd -