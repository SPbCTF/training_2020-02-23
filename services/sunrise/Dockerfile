FROM ubuntu:latest
EXPOSE 3771/tcp
RUN apt-get update
RUN apt-get install -y socat
CMD ["sh","-c","cd /home/task/;ls -anl;chmod +x task_serv;./runserver.sh"]
