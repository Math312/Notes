docker build -t mybatis-demo-mysql .
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 --name mybatis-mysql mybatis-demo-mysql:latest
