curl -X POST -H "${HEADER}" --data "${DATA}" --cert /etc/kafka/secrets/connect.certificate.pem --key /etc/kafka/secrets/connect.key --tlsv1.2 --cacert /etc/kafka/secrets/snakeoil-ca-1.crt -u connectorSubmitter:connectorSubmitter https://connect:8083/connectors

curl -X GET -H "Content-Type: application/json" \
    --cert /Users/mm10444/Projects/streaming-platform/sp-connector-manager/scripts/security/connect-manager.certificate.pem \
    --key /Users/mm10444/Projects/streaming-platform/sp-connector-manager/scripts/security/connect-manager.key \
    --cacert /Users/mm10444/Projects/streaming-platform/sp-connector-manager/scripts/security/snakeoil-ca-1.crt \
    -u connectorSubmitter:connectorSubmitter https://localhost:8083/connectors
    

## Build the Connect Manager Docker Image
First task is to build your connect manager. So go ahead and run the following build command which will make your image made. Please note that this image needs to download specific certs from MassMutual artifactory. Therefore, it is required to be on the VPN.
``` bash
docker image build --tag localbuild/connect-manager:5.5.1-5.5.0 .
```

## Get off the VPN
Now that you have successfully built your local application, `connect-manager`, we need to get going with the provisioning Confluent components. Assuming that the images that `docker-compose.yml` file refers to has never been built by your system, it will need to download and access certain servers from the internet. However, I have noticed that being on VPN doesn't allow the access. You might want to get off the VPN to download and build images. We know it is safe to do so because we are using Confluent popular repository `[cp-demo](https://github.com/confluentinc/cp-demo)`. Once you are off the VPN, run the following command to get your Confluent Platform up and running in your Docker runtime!

```
CLEAN=true ./scripts/start.sh
```

This command will do two things essentially. 
1. Regenerate (if already present) your certificates for all the clients involved in the demo.
2. And bascially run `docker-compose -d <container names in yaml file>` one by one.

For the most part, you should not need to worry about it once you launch the command. It might take some time because its download and building the images for you locally.

## Run LocalTunnel to access Confluent Connect server from the Internet
Since the `Confluent Connect` is running in its own Docker container we need to somehow make it accessible for `Connect Manager` (which runs on K8) so that it can talk to it for starting and stopping connectors.

Start a new terminal and run the following command (from the project directory)

``` bash
npx localtunnel --local-https true \
    --local-cert ./scripts/security/connect-manager.certificate.pem \
    --local-key ./scripts/security/connect-manager.key \
    --local-ca ./scripts/security/snakeoil-ca-1.crt \
    --port 8083
```

**Note**: This demo requires npx installed. Please refer to [this page](https://github.com/localtunnel/localtunnel#quickstart) to understand how to install installation `localtunnel`.

`Confluent Connect` service runs on 8083 mapped port on localhost. So simply allow (HTTPS) connections to it such that it uses the certs you have given it in the command. The command will output a random DNS address. Now, Connect Server can be hit from the internet using that address!

Also note, since `Confluent Connect` server runs on STRE AWS and all the resources in MassMutual can talk to each other, the LocalTunnel step above is only so that application running on Kubernetes (`Connect Manager`) can talk to it via internet.

## Start Connect Manager service on Kubernetes

```bash
kubectl create -f connect-manager-deployment.yaml
```

### Create a service 
This will map a port on your node (localhost) route requests to internal container port of 8080. This mechanism is provisioned by specifying the type of the service as `NodePort`.

```bash
kubectl expose deployment connect-manager --port 8080 --name connect-manager-service --type NodePort
```

## Test!
Open up a new terminal and curl the following command which gets all the running connectors. This should output an empty collection since we have not started any connectors yet. But it didn't error out! 

``` bash
curl -X GET -H "Content-Type: application/json" \
    -u connectorSubmitter:connectorSubmitter https://<Local Tunnel Random DNS>/connectors
```

curl -H 'Authorization: token cea118578a3984a089c5714e3351101883c0eecf' \
  -H 'Accept: application/vnd.github.v3.raw' \
  -O \
  -L https://api.github.com/repos/kgupta26/sp-connector-manager/contents/state/connector/state.json?ref=develop

  curl -H 'Authorization: token cea118578a3984a089c5714e3351101883c0eecf' \
  -H 'Accept: application/vnd.github.v3.raw' \
  -O \
  -L https://api.github.com/repos/kgupta26/sp-connector-manager/contents/Dockerfile

  curl -X GET -H "Content-Type: application/json" \
    -u connectorSubmitter:connectorSubmitter https://localhost:8083/connectors
    http://https//:localhost:8083