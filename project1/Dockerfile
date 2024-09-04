FROM ubuntu:22.04
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get -y update
RUN apt-get -y upgrade

RUN apt install -y postgresql postgresql-contrib libpq-dev
RUN apt install -y python3-pip python3-dev
RUN apt install python-is-python3
RUN apt install -y build-essential vim 
RUN apt install -y sudo curl systemctl
RUN pip3 install ipython-sql==0.4.1 psycopg2==2.9.5 SQLAlchemy==1.4.46

ADD *.sql /home/project1/
ADD *.py /home/project1/

EXPOSE 8888
EXPOSE 5432


USER postgres

RUN /etc/init.d/postgresql start &&\
    createdb flights &&\
    psql --command "\i /home/project1/small.sql;" flights &&\
    psql --command "create user root;" flights &&\
    psql --command "alter user root with password 'root';" flights &&\
    psql --command "alter user root with superuser;"

USER root

RUN echo "listen_addresses = '*'" >> /etc/postgresql/14/main/postgresql.conf &&\
    echo "host    all             all             0.0.0.0/0               scram-sha-256" >> /etc/postgresql/14/main/pg_hba.conf

WORKDIR /home/project1
ENTRYPOINT service postgresql start && /bin/bash
