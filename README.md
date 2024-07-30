# Итоговый проект "Поисковый движок"
## Описание проекта
Данный движок написан на языке Java и реализует индексацию сайтов по url. Принцип работы заключается в постраничном обходе всех внутренних ссылок найденных на каждой странице из списка сайтов указанных в конфигурационном файле application.yaml. Обход выполняется в многопотоковом режиме. Процесс обхода может быть прерван в любой момент, но следует учитывать, что процесс прерывается не мгновенно, а по факту завершения работы уже запущенных потоков. Узнать текущий статус индексации и количество выполненной работы можно на главной странице UI. Данные сайтов проходят процесс лемматизации и сохраняются в соответствующих таблицах БД.

## Возможности:

* Управление по средствам Web UI;
* Постраничный обход сайтов, url которых указан в файле application.yaml секция indexing-settings -> sites;
* Индексация содержимого страниц (для индексации учитываются только слова русского языка);
* Вывод статусов индексации и статистической информации на Web UI;
* Возможность индексации или обновления отдельной страницы;
* Поиск ключевых слов на проиндексированных сайтах и отображение его списка.

## Особенности
* Движок работает с данными на русском языке. Слова написанные латинскими символами и не соответствующие диапазону [А-Яа-я] игнорируются и не будут учтены в результатах поиска.
* Размер сниппета на странице поиска имеет ограниченную длину, соответственно не все найденные ключевые слова будут в нем отображены.
* 

## Стек технологий
