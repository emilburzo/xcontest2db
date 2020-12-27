FROM openjdk:8-jdk

RUN mkdir /app

ADD build/libs/xcontest2db-1.0-SNAPSHOT-all.jar /app/xcontest2db.jar

CMD [ "java", "-jar", "/app/xcontest2db.jar" ]