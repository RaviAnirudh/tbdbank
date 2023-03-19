FROM amazoncorretto:11

LABEL maintainer="smatla@uwaterloo.ca"

CMD ["--name", "tbdBank-container"]

COPY . /app
COPY gradlew /app/gradlew
COPY gradle /app/gradle

ARG REDIS_HOST=""
ENV REDIS_HOST ${REDIS_HOST}


WORKDIR /app
EXPOSE 8081


RUN chmod +x ./gradlew && \
    ./gradlew build

RUN mkdir -p /var/config
COPY tbdbank.properties /var/config/tbdbank.properties

RUN mkdir -p /app && \
    cp ./build/libs/TBDBank.jar /app/TBDBank.jar

CMD ["java", "-Dspring.redis.host=${REDIS_HOST}", \
        "-jar", "/app/TBDBank.jar"]
