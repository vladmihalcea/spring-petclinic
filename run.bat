set MYSQL_URL=jdbc:mysql://localhost/petclinic?useSSL=false
set MYSQL_USER=mysql
set MYSQL_PASS=admin
rem set JVM_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
set JVM_OPTS=
java -jar -D"spring.profiles.active"=mysql %JVM_OPTS% .\target\spring-petclinic-4.0.0-SNAPSHOT.jar