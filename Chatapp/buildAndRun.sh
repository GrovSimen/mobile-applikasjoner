#!/bin/sh
mvn clean package && docker build -t no.ntnu.tollefsen/Chat2018 .
docker rm -f Chat2018 || true && docker run -d -p 8080:8080 -p 4848:4848 --name Chat2018 no.ntnu.tollefsen/Chat2018
