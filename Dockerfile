FROM openjdk:8-alpine

WORKDIR /app

COPY ./Solution.java /app/Solution.java

RUN ["javac", "-encoding", "utf-8", "/app/Solution.java"]

CMD ["java", "-Dfile.encoding=UTF-8", "-cp", "/app", "Solution", "twoSum" , "1:[1,2,3,4]" ,"10:3"]