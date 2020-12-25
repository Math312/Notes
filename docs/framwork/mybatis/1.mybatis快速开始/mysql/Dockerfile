FROM mysql:5.7
WORKDIR /docker-entrypoint-initdb.d
COPY utf8mb4.cnf /etc/mysql/conf.d/utf8mb4.cnf
ADD init.sql /docker-entrypoint-initdb.d/