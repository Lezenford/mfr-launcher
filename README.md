# Morrowind Fullrest Repack Launcher

***Что это?***

Morrowind Fullrest Repack Launcher - это файловый менеджер специально написанный для управления сборками игры TES III
Morrowind от команды fullrest.ru.

Оно включает в себя серверную и десктопную части и позволяет осуществлять пользовательскую настройку игрового окружения,
а так же поддерживает автоматическое обновление через сеть Интернет.

***Как это работает?***

Данный проект писался для конкретной команды разработчиков и моделлеров. Он требует опреденной файловой структуры внутри
папки с игрой.

Не смотря на это, данный софт можно использовать для создания и сопровождения собственного проекта. Файлы конфигурации
позволяют частично изменять требования к поддерживаемой файловой структуре.

Серверная часть публикует публичное API для предоставление обновлений и контроля версий игры.

Десктопное приложение выполняет функции файлового менеджера для перемещения и замены файлов игры, согласно заданной
схеме.

***Настройка серверной части***
Для использования сертификатов от Let's Encrypt необходимо использовать в качестве цепочки сертификатов файл
fullchain.pem и в качестве ключа к нему сконвертированный приватный ключ key.pem. Конвертация
командой `openssl pkcs8 -topk8 -in key.pem -out private.pem -nocrypt`

***Настройка десктопной части***
Для использования системного трея запускать клиент нужно с опцией `-Djava.awt.headless=false`