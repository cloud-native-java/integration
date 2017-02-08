#!/bin/bash


server_jar=`dirname $0`/cfdf-server.jar

server_redis=cfdf-redis
server_mysql=cfdf-mysql
server_rabbit=cfdf-rabbit


app_name=cfdf

cf d -f $app_name && echo "deleted existing Spring Cloud Data Flow Cloud Foundry Server."


# is it a release or a snapshot
#server_jar_version=1.2.0.BUILD-SNAPSHOT
server_jar_version=1.1.0.RELEASE
server_jar_version=1.1.0.BUILD-SNAPSHOT

server_jar_url_prefix=snapshot

if [[ $server_jar_version =~ .*RELEASE.* ]]
then
   server_jar_url_prefix=release
fi

server_jar_url=http://repo.spring.io/${server_jar_url_prefix}/org/springframework/cloud/spring-cloud-dataflow-server-cloudfoundry/${server_jar_version}/spring-cloud-dataflow-server-cloudfoundry-${server_jar_version}.jar

echo "attempting to download Spring Cloud Data Flow server binary from ${server_jar_url}. ";

[ -f ${server_jar} ] || wget -O ${server_jar} "${server_jar_url}"
[ -f ${server_jar} ] && echo "cached ${server_jar} locally."

cf push $app_name -m 2G -k 2G --no-start -p ${server_jar}



cf s | grep $server_redis || cf cs rediscloud 30mb $server_redis
cf s | grep $server_mysql || cf cs p-mysql 100mb $server_mysql
cf s | grep $server_rabbit || cf cs cloudamqp lemur $server_rabbit

cf bind-service $app_name $server_redis
cf bind-service $app_name $server_mysql

cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SKIP_SSL_VALIDATION false
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_URL https://api.run.pivotal.io
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_ORG $CF_ORG
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SPACE $CF_SPACE
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_DOMAIN cfapps.io
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_SERVICES $server_rabbit
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_TASK_SERVICES $server_mysql
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_USERNAME $CF_USER
cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_PASSWORD $CF_PASSWORD

cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_LR_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-release
cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_LS_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-snapshot
cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_PR_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-release
cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_PS_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-snapshot

cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_INSTANCES 1

cf start $app_name
