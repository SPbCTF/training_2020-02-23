# Сервис payconsul

[Роман Опякин](https://t.me/sinketsu)

### Agenda
Сервис изначально задумывался как "аналог" [Hashicorp Consul](https://www.consul.io/)
Реализована функциональность `key-value` хранилища, регистрации/дерегистрации сервисов в каталоге, а также управление хэлсчеками.
Также по легенде чуваки запилили новую версию и выкатили ее под балансер. Туда попадает 20% траффика пользователей (управляется кукой).

#### Язык
`Go (1.13+)`

#### Build & run

Перед началом игры, необходимо раскидать по командам их уникальные токены. Положить их в `<service>/checker_token` и добавить в 
список `TOKENS` в чекере. (Необходим перезапуск сервиса при изменении токена).

```shell script
docker-compose up -d
```

#### Баги
1. Кривой `alias` в конфиге nginx. Позволяет через `Path traversal` гетнуть всю базу.
    ```
    Фиксится добавлением слеша в конец пути - 
   location /static/ {
    ```
2. SQL injection в `key-value` (конфиг nginx, кастомные обработчики на `Lua`)
    ```
    Переписать кусок самим, либо перейти на prepared queries в Lua.
    ```

3. Закладка в `/metrics`. На одном из апстримов (`ID == 2`) запускается цикл, который выводит последний флаг из базы.
    ```
    Убрать эту функциональность (vendor/github.com/sirupsen/logrus/exported.go)
    Либо перейти с vendor на скачивание зависимостей.
    ```

4. Можно фармить квоту для покупки чужих сервисов через `/v1/deregister` несуществующего сервиса.
    ```
    Проверять, что сервис действительно существует.
    Тут есть сложности уже с sqlite3. База по факту просто файлик, который постоянно лочится на транзакциях.
    Можно перенести на какой-нить MySQL. В go это делается заменой драйвера + исправлением SQL запросов.
    ```

5. Можно было ложить сервис другим тимам проставляя неправильный хэлсчек между проверками чекера.
Чекер регистрировал сервис, потом ждал 5 секунд, потом проверяет хэлсчек. Так как никаких `ACL` на хэлсчеки нет. 
То можно успеть пометить сервис чекера как проваливший чек. Тем самым увести у другой тимы в `MUMBLE`
    ```
    Добавить ACL на проставление хэлсчека только своему сервису.
    ```
   
   
#### Чекер
Чекер проверяет:
1. Экспорт метрик (по `promhttp_metric_handler_requests_total{code="200"}`)
2. Что проставляется кука с апстримом (`upid`)
3. Что по куке попадаешь в разные апстримы
4. Что можно зарегать сервис
5. Что можно поставить хэлсчек сервису
6. Что нельзя зарегать одного юзера/один сервис дважды
7. Что нельзя дерегистрировать чужой сервис
8. Что работает `key-value` хранилище
9. Что можно дерегистрировать сервис

Чекер кладет флаги в разные места:
* `key-value` хранилище с вероятностью 25%
* в поле `meta` сервиса с вероятностью 75%