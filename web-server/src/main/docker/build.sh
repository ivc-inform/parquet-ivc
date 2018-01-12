#docker build --no-cache --force-rm --tag ivcinform/tss:0.0.1.1 .
#
#docker run -d -p 8085:8085 --add-host hadoop-ipc-host:172.18.0.2 --restart on-failure:5 --name tss_011 --network hadoop ivcinform/template-storage-service:0.0.1-SNAPSHOT
