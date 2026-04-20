FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Копируем Maven wrapper
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Копируем все pom.xml (нужны для разрешения зависимостей между модулями)
COPY pom.xml .
COPY core/pom.xml core/
COPY cache_module/pom.xml cache_module/
COPY image_module/pom.xml image_module/
COPY product_module/pom.xml product_module/
COPY user_module/pom.xml user_module/
COPY order_module/pom.xml order_module/
COPY comment_module/pom.xml comment_module/
COPY test_data_module/pom.xml test_data_module/
COPY app/pom.xml app/

# Копируем только исходный код основного модуля app и тех модулей, от которых он зависит
COPY app/src app/src/
COPY test_data_module/src test_data_module/src/
COPY order_module/src order_module/src/
COPY comment_module/src comment_module/src/
COPY product_module/src product_module/src/
COPY user_module/src user_module/src/
COPY image_module/src image_module/src/
COPY core/src core/src/
COPY cache_module/src cache_module/src/

# Собираем только модуль app
RUN ./mvnw clean package -DskipTests -pl app -am

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN mkdir -p /app/logs

COPY --from=build /app/app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 8080