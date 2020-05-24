FROM openjdk:8-jdk

RUN mkdir /app

ADD target/xcontest2db-1.0-SNAPSHOT-jar-with-dependencies.jar /app/xcontest2db.jar

CMD [ "java", "-jar", "/app/xcontest2db.jar" ]